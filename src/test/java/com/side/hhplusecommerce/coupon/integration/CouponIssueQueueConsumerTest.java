package com.side.hhplusecommerce.coupon.integration;

import com.side.hhplusecommerce.ContainerTest;
import com.side.hhplusecommerce.coupon.domain.Coupon;
import com.side.hhplusecommerce.coupon.domain.CouponStock;
import com.side.hhplusecommerce.coupon.domain.UserCoupon;
import com.side.hhplusecommerce.coupon.infrastructure.redis.CouponIssueQueueConsumer;
import com.side.hhplusecommerce.coupon.infrastructure.redis.CouponIssueQueueProducer;
import com.side.hhplusecommerce.coupon.repository.CouponRepository;
import com.side.hhplusecommerce.coupon.repository.CouponStockRepository;
import com.side.hhplusecommerce.coupon.repository.UserCouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.side.hhplusecommerce.coupon.infrastructure.redis.CouponIssueQueueConstants.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class CouponIssueQueueConsumerTest extends ContainerTest {

    @Autowired
    private CouponIssueQueueConsumer consumer;

    @Autowired
    private CouponIssueQueueProducer producer;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponStockRepository couponStockRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private Long couponId;
    private Long userId;

    @BeforeEach
    void setUp() {
        couponId = 1L;
        userId = 100L;

        // Redis 초기화
        try {
            // Consumer Group 삭제 (존재하는 경우)
            redisTemplate.opsForStream().destroyGroup(COUPON_ISSUE_QUEUE, CONSUMER_GROUP);
        } catch (Exception ignored) {
            // 그룹이 없으면 무시
        }

        redisTemplate.delete(COUPON_ISSUE_QUEUE);
        redisTemplate.delete(COUPON_STOCK_PREFIX + couponId);
        redisTemplate.delete(COUPON_ISSUED_USERS_PREFIX + couponId);

        // Consumer Group 초기화 (Consumer의 @PostConstruct 강제 실행)
        consumer.init();

        // 쿠폰 데이터 준비
        Coupon coupon = Coupon.builder()
                .name("테스트 쿠폰")
                .discountAmount(5000)
                .totalQuantity(10)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
        Coupon saved = couponRepository.save(coupon);
        couponId = saved.getCouponId();

        CouponStock stock = CouponStock.of(couponId, 10);
        couponStockRepository.save(stock);

        // Redis 재고 초기화
        redisTemplate.opsForValue().set(COUPON_STOCK_PREFIX + couponId, 10);
    }

    @Test
    @DisplayName("[성공] Queue에서 메시지를 읽어 MySQL에 쿠폰 발급")
    void processBatch_success() {
        // given
        producer.enqueue(couponId, userId);

        // when
        int processedCount = consumer.processBatch();

        // then
        assertThat(processedCount).isEqualTo(1);

        // MySQL에 쿠폰이 발급되었는지 확인
        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId);
        assertThat(userCoupons).hasSize(1);
        assertThat(userCoupons.get(0).getCouponId()).isEqualTo(couponId);

        // 재고 차감 확인
        CouponStock stock = couponStockRepository.findByCouponId(couponId).orElseThrow();
        assertThat(stock.getRemainingQuantity()).isEqualTo(9);
    }

    @Test
    @DisplayName("[성공] Redis Set과 재고가 MySQL 처리 후 업데이트됨")
    void processBatch_updatesRedis() {
        // given
        producer.enqueue(couponId, userId);

        // when
        consumer.processBatch();

        // then
        // Redis Set에 사용자 추가됨
        Boolean isMember = redisTemplate.opsForSet()
                .isMember(COUPON_ISSUED_USERS_PREFIX + couponId, userId.toString());
        assertThat(isMember).isTrue();

        // Redis 재고 차감됨
        Integer stock = (Integer) redisTemplate.opsForValue().get(COUPON_STOCK_PREFIX + couponId);
        assertThat(stock).isEqualTo(9);
    }

    @Test
    @DisplayName("[실패] 재고 부족 시 발급 실패하지만 ACK 처리됨")
    void processBatch_outOfStock() {
        // given
        // 재고를 0으로 설정
        CouponStock stock = couponStockRepository.findByCouponId(couponId).orElseThrow();
        for (int i = 0; i < 10; i++) {
            stock.decrease();
        }
        couponStockRepository.save(stock);

        producer.enqueue(couponId, userId);

        // when
        int processedCount = consumer.processBatch();

        // then
        // 처리는 실패했지만 0이 아님 (예외 발생했으나 catch됨)
        assertThat(processedCount).isZero();

        // 쿠폰이 발급되지 않음
        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId);
        assertThat(userCoupons).isEmpty();
    }

    @Test
    @DisplayName("[성공] 중복 발급 시도 시 MySQL에서 검증하여 1개만 발급")
    void processBatch_duplicateRequest() {
        // given
        // 같은 사용자가 2번 Queue에 추가 (Redis 필터링을 우회했다고 가정)
        Map<String, Object> message = Map.of(
                FIELD_USER_ID, userId.toString(),
                FIELD_COUPON_ID, couponId.toString(),
                FIELD_REQUEST_TIME, LocalDateTime.now().toString()
        );
        redisTemplate.opsForStream().add(COUPON_ISSUE_QUEUE, message);
        redisTemplate.opsForStream().add(COUPON_ISSUE_QUEUE, message);

        // when
        consumer.processBatch();

        // then
        // MySQL에서 중복 검증하여 1개만 발급됨
        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId);
        assertThat(userCoupons).hasSize(1);

        // 재고는 1개만 차감
        CouponStock stock = couponStockRepository.findByCouponId(couponId).orElseThrow();
        assertThat(stock.getRemainingQuantity()).isEqualTo(9);
    }
}
