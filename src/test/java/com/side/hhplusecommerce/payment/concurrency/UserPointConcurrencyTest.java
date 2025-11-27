package com.side.hhplusecommerce.payment.concurrency;

import com.side.hhplusecommerce.ContainerTest;
import com.side.hhplusecommerce.payment.service.UserPointService;
import com.side.hhplusecommerce.point.domain.UserPoint;
import com.side.hhplusecommerce.point.repository.UserPointRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UserPointConcurrencyTest extends ContainerTest {

    @Autowired
    private UserPointService userPointService;

    @Autowired
    private UserPointRepository userPointRepository;

    @Test
    @DisplayName("동일한 사용자에 대해 50번의 포인트 충전을 동시에 시도하면, 모두 성공하고 최종 포인트는 정확히 계산된다")
    void concurrentCharge_shouldSucceedAllAndCalculateCorrectly() throws InterruptedException {
        // given
        Long userId = 1L;
        int chargeAmount = 1000;
        int threadCount = 50;

        // 초기 포인트 생성
        UserPoint userPoint = UserPoint.initialize(userId);
        userPointRepository.save(userPoint);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        executeConcurrently(threadCount, (index) -> {
            try {
                userPointService.charge(userId, chargeAmount);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
                System.err.println("충전 실패: " + e.getMessage());
            }
        });

        // then
        UserPoint result = userPointRepository.findByUserId(userId).orElseThrow();
        int expectedPoint = chargeAmount * threadCount;

        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(failCount.get()).isZero();
        assertThat(result.getPoint()).isEqualTo(expectedPoint);

        printResult("포인트 충전", threadCount, successCount.get(), failCount.get(), expectedPoint, result.getPoint());
    }

    @Test
    @DisplayName("동일한 사용자에 대해 30번의 포인트 사용을 동시에 시도하면, 모두 성공하고 최종 포인트는 정확히 계산된다")
    void concurrentUse_shouldSucceedAllAndCalculateCorrectly() throws InterruptedException {
        // given
        Long userId = 2L;
        int initialPoint = 100000;
        int useAmount = 1000;
        int threadCount = 30;

        // 초기 포인트 생성 및 충전
        UserPoint userPoint = UserPoint.initialize(userId);
        userPoint.charge(initialPoint);
        userPointRepository.save(userPoint);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        executeConcurrently(threadCount, (index) -> {
            try {
                userPointService.use(userId, useAmount);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
                System.err.println("사용 실패: " + e.getMessage());
            }
        });

        // then
        UserPoint result = userPointRepository.findByUserId(userId).orElseThrow();
        int expectedPoint = initialPoint - (useAmount * threadCount);

        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(failCount.get()).isZero();
        assertThat(result.getPoint()).isEqualTo(expectedPoint);

        printResult("포인트 사용", threadCount, successCount.get(), failCount.get(), expectedPoint, result.getPoint());
    }

    @Test
    @DisplayName("동일한 사용자에 대해 충전과 사용을 동시에 시도하면, 모두 성공하고 최종 포인트는 정확히 계산된다")
    void concurrentChargeAndUse_shouldSucceedAllAndCalculateCorrectly() throws InterruptedException {
        // given
        Long userId = 3L;
        int initialPoint = 50000;
        int chargeAmount = 2000;
        int useAmount = 1000;
        int chargeThreadCount = 20;
        int useThreadCount = 30;
        int totalThreadCount = chargeThreadCount + useThreadCount;

        // 초기 포인트 생성 및 충전
        UserPoint userPoint = UserPoint.initialize(userId);
        userPoint.charge(initialPoint);
        userPointRepository.save(userPoint);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        ExecutorService executor = Executors.newFixedThreadPool(totalThreadCount);
        CyclicBarrier barrier = new CyclicBarrier(totalThreadCount);
        CountDownLatch latch = new CountDownLatch(totalThreadCount);

        try {
            // 충전 스레드
            for (int i = 0; i < chargeThreadCount; i++) {
                executor.submit(() -> {
                    try {
                        barrier.await();
                        userPointService.charge(userId, chargeAmount);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                        System.err.println("충전 실패: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // 사용 스레드
            for (int i = 0; i < useThreadCount; i++) {
                executor.submit(() -> {
                    try {
                        barrier.await();
                        userPointService.use(userId, useAmount);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                        System.err.println("사용 실패: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean completed = latch.await(60, TimeUnit.SECONDS);
            assertThat(completed).withFailMessage("테스트 타임아웃").isTrue();
        } finally {
            executor.shutdown();
        }

        // then
        UserPoint result = userPointRepository.findByUserId(userId).orElseThrow();
        int expectedPoint = initialPoint + (chargeAmount * chargeThreadCount) - (useAmount * useThreadCount);

        assertThat(successCount.get()).isEqualTo(totalThreadCount);
        assertThat(failCount.get()).isZero();
        assertThat(result.getPoint()).isEqualTo(expectedPoint);

        printResult("포인트 충전/사용 혼합", totalThreadCount, successCount.get(), failCount.get(), expectedPoint, result.getPoint());
    }
    
    private void executeConcurrently(int threadCount, TaskWithIndex task) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        try {
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        barrier.await();
                        task.run(index);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean completed = latch.await(60, TimeUnit.SECONDS);
            assertThat(completed).withFailMessage("테스트 타임아웃: 60초 내에 완료되지 않음").isTrue();
        } finally {
            executor.shutdown();
        }
    }

    private void printResult(String testName, int totalAttempts, int successCount, int failCount, int expectedPoint, int actualPoint) {
        System.out.println("=".repeat(60));
        System.out.println(testName + " 동시성 테스트 결과");
        System.out.println("=".repeat(60));
        System.out.println(String.format("총 시도:       %d", totalAttempts));
        System.out.println(String.format("성공:          %d", successCount));
        System.out.println(String.format("실패:          %d", failCount));
        System.out.println(String.format("예상 포인트:   %d", expectedPoint));
        System.out.println(String.format("실제 포인트:   %d", actualPoint));
        System.out.println(String.format("정확성:        %s", expectedPoint == actualPoint ? "✓" : "✗"));
        System.out.println("=".repeat(60));
    }

    @FunctionalInterface
    interface TaskWithIndex {
        void run(int index);
    }
}