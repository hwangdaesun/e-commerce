package com.side.hhplusecommerce.coupon.integration;

import com.side.hhplusecommerce.ContainerTest;
import com.side.hhplusecommerce.cart.usecase.CouponIssueUseCase;
import com.side.hhplusecommerce.coupon.domain.Coupon;
import com.side.hhplusecommerce.coupon.domain.CouponStock;
import com.side.hhplusecommerce.coupon.domain.UserCoupon;
import com.side.hhplusecommerce.coupon.infrastructure.redis.CouponIssueQueueConsumer;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.side.hhplusecommerce.coupon.infrastructure.redis.CouponIssueQueueConstants.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Queue 기반 쿠폰 발급 통합 테스트
 * Producer → Queue → Consumer → MySQL 전체 흐름을 검증합니다.
 */
@SpringBootTest
class CouponQueueIntegrationTest extends ContainerTest {

    @Autowired
    private CouponIssueUseCase couponIssueUseCase;

    @Autowired
    private CouponIssueQueueConsumer consumer;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponStockRepository couponStockRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private Long testCouponId;        // 테스트용 쿠폰 ID (재고 10개)
    private Long largeCouponId;       // 대용량 테스트용 쿠폰 ID (재고 100개)

    @BeforeEach
    void setUp() {
        // Consumer Group 초기화
        try {
            // Consumer Group 삭제 (존재하는 경우)
            redisTemplate.opsForStream().destroyGroup(COUPON_ISSUE_QUEUE, CONSUMER_GROUP);
        } catch (Exception ignored) {
            // 그룹이 없으면 무시
        }

        // Redis Stream 초기화
        redisTemplate.delete(COUPON_ISSUE_QUEUE);

        // Consumer Group 생성 (Consumer의 @PostConstruct 강제 실행)
        consumer.init();

        // 테스트용 쿠폰 생성
        testCouponId = createCoupon("테스트 쿠폰", 10);
        largeCouponId = createCoupon("대용량 테스트 쿠폰", 100);

        // 각 쿠폰별 Redis 키 초기화
        cleanupCouponRedisKeys(testCouponId);
        cleanupCouponRedisKeys(largeCouponId);

        // Redis 재고 초기화
        redisTemplate.opsForValue().set(COUPON_STOCK_PREFIX + testCouponId, 10);
        redisTemplate.opsForValue().set(COUPON_STOCK_PREFIX + largeCouponId, 100);
    }

    /**
     * 쿠폰 생성 헬퍼 메서드
     */
    private Long createCoupon(String name, int totalStock) {
        Coupon coupon = Coupon.builder()
                .name(name)
                .discountAmount(5000)
                .totalQuantity(totalStock)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
        Long couponId = couponRepository.save(coupon).getCouponId();

        CouponStock stock = CouponStock.of(couponId, totalStock);
        couponStockRepository.save(stock);

        return couponId;
    }

    /**
     * 특정 쿠폰의 Redis 키 정리
     */
    private void cleanupCouponRedisKeys(Long couponId) {
        redisTemplate.delete(COUPON_STOCK_PREFIX + couponId);
        redisTemplate.delete(COUPON_ISSUED_USERS_PREFIX + couponId);
    }

    @Test
    @DisplayName("[통합] Producer → Queue → Consumer → MySQL 전체 흐름 검증")
    void fullFlow_producerToConsumer() {
        // given
        Long userId = 100L;

        // when
        // 1. Producer: Queue에 추가
        couponIssueUseCase.issue(testCouponId, userId);

        // 2. Consumer: 배치 처리
        int processedCount = consumer.processBatch();

        // then
        assertThat(processedCount).isEqualTo(1);

        // 3. MySQL 확인
        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId);
        assertThat(userCoupons).hasSize(1);

        CouponStock stock = couponStockRepository.findByCouponId(testCouponId).orElseThrow();
        assertThat(stock.getRemainingQuantity()).isEqualTo(9);

        // 4. Redis 동기화 확인
        Boolean isMember = redisTemplate.opsForSet()
                .isMember(COUPON_ISSUED_USERS_PREFIX + testCouponId, userId.toString());
        assertThat(isMember).isTrue();

        Integer redisStock = (Integer) redisTemplate.opsForValue().get(COUPON_STOCK_PREFIX + testCouponId);
        assertThat(redisStock).isEqualTo(9);
    }

    @Test
    @DisplayName("[동시성] 100명 동시 요청 → Consumer 처리 → 정확히 10명만 발급")
    void concurrency_fullFlow() throws InterruptedException {
        // given
        int totalStock = 10;
        int threadCount = 100;
        Long couponId = testCouponId; // setUp에서 생성된 쿠폰 사용

        // Redis 재고는 setUp에서 이미 초기화됨

        // when: 100명이 동시에 요청 (Queue에 추가)
        executeConcurrently(threadCount, () -> {
            try {
                Long userId = Thread.currentThread().getId();
                couponIssueUseCase.issue(couponId, userId);
            } catch (Exception ignored) {
                // Redis 필터링으로 거부된 요청은 무시
            }
        });

        // Consumer가 Queue의 모든 메시지를 처리할 때까지 반복
        int totalProcessed = 0;
        for (int i = 0; i < 10; i++) { // 최대 10번 시도
            int processed = consumer.processBatch();
            totalProcessed += processed;
            if (processed == 0) break; // 더 이상 처리할 메시지 없음
            Thread.sleep(100); // 약간의 대기
        }

        // then: MySQL에서 정확히 10개만 발급됨
        CouponStock stock = couponStockRepository.findByCouponId(couponId).orElseThrow();
        assertThat(stock.getRemainingQuantity()).isZero();

        // 발급된 쿠폰 수 확인
        List<UserCoupon> allIssuedCoupons = userCouponRepository.findAll();
        long issuedCount = allIssuedCoupons.stream()
                .filter(uc -> uc.getCouponId().equals(couponId))
                .count();
        assertThat(issuedCount).isEqualTo(totalStock);

        System.out.println("총 처리된 메시지: " + totalProcessed);
        System.out.println("실제 발급된 쿠폰: " + issuedCount);
    }

    @Test
    @DisplayName("[동시성] 같은 사용자가 동시에 10번 요청 → 1번만 성공")
    void concurrency_sameUser() throws InterruptedException {
        // given
        Long userId = 1L;
        int totalStock = 100;
        int threadCount = 10;
        Long couponId = largeCouponId; // setUp에서 생성된 대용량 쿠폰 사용

        // Redis 재고는 setUp에서 이미 초기화됨

        // when: 같은 사용자가 10번 동시 요청
        executeConcurrently(threadCount, () -> {
            try {
                couponIssueUseCase.issue(couponId, userId);
            } catch (Exception ignored) {
                // 중복 요청은 무시
            }
        });

        // Consumer 처리
        for (int i = 0; i < 5; i++) {
            int processed = consumer.processBatch();
            if (processed == 0) break;
            Thread.sleep(100);
        }

        // then: 1개만 발급됨
        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId);
        assertThat(userCoupons).hasSize(1);

        CouponStock stock = couponStockRepository.findByCouponId(couponId).orElseThrow();
        assertThat(stock.getRemainingQuantity()).isEqualTo(totalStock - 1);
    }

    @Test
    @DisplayName("[재처리] Pending 메시지가 재처리됨")
    void pendingMessage_reprocessing() {
        // given
        Long couponId = testCouponId; // setUp에서 생성된 쿠폰 사용
        Long userId = 100L;

        // Redis 재고는 setUp에서 이미 초기화됨

        // Queue에 추가
        couponIssueUseCase.issue(couponId, userId);

        // 첫 번째 처리 (성공)
        consumer.processBatch();

        // then: Pending 메시지 없음
        int pendingProcessed = consumer.processPendingMessages();
        assertThat(pendingProcessed).isZero();
    }

    /**
     * 여러 스레드를 동시에 시작하여 작업을 실행하는 헬퍼 메서드
     */
    private void executeConcurrently(int threadCount, Runnable task) throws InterruptedException {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadCount);
        CyclicBarrier startBarrier = new CyclicBarrier(threadCount);
        CountDownLatch completeLatch = new CountDownLatch(threadCount);

        try {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startBarrier.await();   // 모든 스레드가 준비될 때까지 대기
                        task.run();             // 실제 작업 실행
                    } catch (Exception e) {
                        // 예외 무시 (테스트에서 예외는 카운터로 처리)
                    } finally {
                        completeLatch.countDown();
                    }
                });
            }

            boolean completed = completeLatch.await(10, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
        } finally {
            executor.shutdown();
        }
    }
}
