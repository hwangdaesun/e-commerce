package com.side.hhplusecommerce.coupon.integration;

import com.side.hhplusecommerce.ContainerTest;
import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.coupon.infrastructure.redis.CouponIssueQueueProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import static com.side.hhplusecommerce.coupon.infrastructure.redis.CouponIssueQueueConstants.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class CouponIssueQueueProducerTest extends ContainerTest {

    @Autowired
    private CouponIssueQueueProducer producer;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private Long couponId;
    private Long userId;

    @BeforeEach
    void setUp() {
        couponId = 1L;
        userId = 100L;

        // Redis 초기화
        redisTemplate.delete(COUPON_ISSUE_QUEUE);
        redisTemplate.delete(COUPON_STOCK_PREFIX + couponId);
        redisTemplate.delete(COUPON_ISSUED_USERS_PREFIX + couponId);
    }

    @Test
    @DisplayName("[성공] Redis 재고 있고 중복 아닌 경우 Queue에 추가 성공")
    void enqueue_success() {
        // given
        redisTemplate.opsForValue().set(COUPON_STOCK_PREFIX + couponId, 10);

        // when
        boolean result = producer.enqueue(couponId, userId);

        // then
        assertThat(result).isTrue();

        // Queue에 메시지가 추가되었는지 확인
        Long queueLength = redisTemplate.opsForStream().size(COUPON_ISSUE_QUEUE);
        assertThat(queueLength).isEqualTo(1L);
    }

    @Test
    @DisplayName("[실패] Redis 재고 없으면 COUPON_SOLD_OUT 예외 발생")
    void enqueue_fail_noStock() {
        // given
        redisTemplate.opsForValue().set(COUPON_STOCK_PREFIX + couponId, 0);

        // when & then
        assertThatThrownBy(() -> producer.enqueue(couponId, userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_SOLD_OUT);

        // Queue에 메시지가 추가되지 않았는지 확인
        Long queueLength = redisTemplate.opsForStream().size(COUPON_ISSUE_QUEUE);
        assertThat(queueLength).isZero();
    }

    @Test
    @DisplayName("[실패] 이미 발급된 쿠폰이면 ALREADY_ISSUED_COUPON 예외 발생")
    void enqueue_fail_alreadyIssued() {
        // given
        redisTemplate.opsForValue().set(COUPON_STOCK_PREFIX + couponId, 10);
        redisTemplate.opsForSet().add(COUPON_ISSUED_USERS_PREFIX + couponId, userId.toString());

        // when & then
        assertThatThrownBy(() -> producer.enqueue(couponId, userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_ISSUED_COUPON);

        // Queue에 메시지가 추가되지 않았는지 확인
        Long queueLength = redisTemplate.opsForStream().size(COUPON_ISSUE_QUEUE);
        assertThat(queueLength).isZero();
    }

    @Test
    @DisplayName("[성공] 여러 사용자가 순차적으로 Queue에 추가 가능")
    void enqueue_multipleUsers() {
        // given
        redisTemplate.opsForValue().set(COUPON_STOCK_PREFIX + couponId, 10);

        // when
        producer.enqueue(couponId, 1L);
        producer.enqueue(couponId, 2L);
        producer.enqueue(couponId, 3L);

        // then
        Long queueLength = redisTemplate.opsForStream().size(COUPON_ISSUE_QUEUE);
        assertThat(queueLength).isEqualTo(3L);
    }
}
