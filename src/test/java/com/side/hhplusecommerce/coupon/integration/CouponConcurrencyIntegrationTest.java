package com.side.hhplusecommerce.coupon.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.side.hhplusecommerce.ContainerTest;
import com.side.hhplusecommerce.cart.usecase.CouponIssueUseCase;
import com.side.hhplusecommerce.coupon.domain.Coupon;
import com.side.hhplusecommerce.coupon.domain.CouponStock;
import com.side.hhplusecommerce.coupon.domain.UserCoupon;
import com.side.hhplusecommerce.coupon.repository.CouponRepository;
import com.side.hhplusecommerce.coupon.repository.CouponStockRepository;
import com.side.hhplusecommerce.coupon.repository.UserCouponRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CouponConcurrencyIntegrationTest extends ContainerTest {

    @Autowired
    private CouponIssueUseCase couponIssueUseCase;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponStockRepository couponStockRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;


    @DisplayName("100명의 사용자가 동시에 선착순 10개 쿠폰을 발급받으면, 정확히 10명만 성공해야 한다")
    @Test
    void concurrentCouponIssuance_shouldIssueExactly10Coupons() throws InterruptedException {
        // given
        int totalStock = 10;
        int threadCount = 100;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // 쿠폰 생성
        Coupon coupon = Coupon.builder()
                .name("선착순 10개 쿠폰")
                .discountAmount(5000)
                .totalQuantity(totalStock)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
        Long couponId = couponRepository.save(coupon).getCouponId();


        // 쿠폰 재고 생성
        CouponStock couponStock = CouponStock.of(couponId, totalStock);
        couponStockRepository.save(couponStock);

        // when
        executeConcurrently(threadCount, () -> {
            try {
                Long userId = Thread.currentThread().getId(); // 각 스레드마다 다른 userId 사용
                couponIssueUseCase.issue(couponId, userId);
                successCount.incrementAndGet();
            } catch (RuntimeException e) {
                failCount.incrementAndGet();
            }
        });

        // then
        int totalAttempts = successCount.get() + failCount.get();
        CouponStock resultStock = couponStockRepository.findByCouponId(couponId).orElseThrow();

        assertThat(totalAttempts).isEqualTo(threadCount); // 모든 시도가 처리됨
        assertThat(successCount.get()).isEqualTo(totalStock); // 정확히 재고 수만큼만 성공
        assertThat(failCount.get()).isEqualTo(threadCount - totalStock); // 나머지는 실패
        assertThat(resultStock.getRemainingQuantity()).isZero(); // 재고가 0이 됨

        System.out.println("쿠폰 발급 테스트 - 성공: " + successCount.get() + ", 실패: " + failCount.get());
    }

    @DisplayName("같은 사용자가 동시에 같은 쿠폰을 여러 번 발급받으려 하면, 1번만 성공해야 한다")
    @Test
    void concurrentCouponIssuance_sameUser_shouldIssueOnlyOnce() throws InterruptedException {
        // given
        Long userId = 1L;
        int totalStock = 100;
        int threadCount = 10;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // 쿠폰 생성
        Coupon coupon = Coupon.builder()
                .name("1인당 1개 제한 쿠폰")
                .discountAmount(5000)
                .totalQuantity(totalStock)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
        Long couponId = couponRepository.save(coupon).getCouponId();

        // 쿠폰 재고 생성
        CouponStock couponStock = CouponStock.of(couponId, totalStock);
        couponStockRepository.save(couponStock);

        // when
        executeConcurrently(threadCount, () -> {
            try {
                couponIssueUseCase.issue(couponId, userId);
                successCount.incrementAndGet();
            } catch (RuntimeException e) {
                failCount.incrementAndGet();
            }
        });

        // then
        int totalAttempts = successCount.get() + failCount.get();
        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId);
        CouponStock resultStock = couponStockRepository.findByCouponId(couponId).orElseThrow();

        assertThat(totalAttempts).isEqualTo(threadCount); // 모든 시도가 처리됨
        assertThat(userCoupons).hasSize(1); // 사용자는 1개의 쿠폰만 보유
        assertThat(resultStock.getRemainingQuantity()).isEqualTo(totalStock - 1); // 재고는 1개만 차감

        System.out.println("중복 발급 방지 테스트 - 성공: " + successCount.get() + ", 실패: " + failCount.get());
    }

    /**
     * 여러 스레드를 동시에 시작하여 작업을 실행하는 헬퍼 메서드
     *
     * @param threadCount 실행할 스레드 수
     * @param task 각 스레드에서 실행할 작업
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
                        e.printStackTrace();
                    } finally {
                        completeLatch.countDown(); // 완료 신호
                    }
                });
            }

            // 모든 스레드가 작업을 완료할 때까지 대기 (최대 10초)
            boolean completed = completeLatch.await(10, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
        } finally {
            executor.shutdown();
        }
    }
}
