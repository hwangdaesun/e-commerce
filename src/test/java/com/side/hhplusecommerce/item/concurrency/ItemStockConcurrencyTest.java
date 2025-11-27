package com.side.hhplusecommerce.item.concurrency;

import com.side.hhplusecommerce.ContainerTest;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.repository.ItemRepository;
import com.side.hhplusecommerce.item.service.ItemStockService;
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
class ItemStockConcurrencyTest extends ContainerTest {

    @Autowired
    private ItemStockService itemStockService;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    @DisplayName("동일한 상품에 대해 50번의 재고 차감을 동시에 시도하면, 모두 성공하고 최종 재고는 정확히 계산된다")
    void concurrentDecrease_shouldSucceedAllAndCalculateCorrectly() throws InterruptedException {
        // given
        int initialStock = 1000;
        int decreaseQuantity = 10;
        int threadCount = 50;

        Item item = Item.builder()
                .name("Test Item")
                .price(10000)
                .stock(initialStock)
                .build();
        Item savedItem = itemRepository.save(item);
        Long itemId = savedItem.getItemId();

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        executeConcurrently(threadCount, (index) -> {
            try {
                itemStockService.decreaseStockForItem(itemId, decreaseQuantity);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
                System.err.println("재고 차감 실패: " + e.getMessage());
            }
        });

        // then
        Item result = itemRepository.findById(itemId).orElseThrow();
        int expectedStock = initialStock - (decreaseQuantity * threadCount);

        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(failCount.get()).isZero();
        assertThat(result.getStock()).isEqualTo(expectedStock);

        printResult("재고 차감", threadCount, successCount.get(), failCount.get(), expectedStock, result.getStock());
    }

    @Test
    @DisplayName("동일한 상품에 대해 30번의 재고 증가를 동시에 시도하면, 모두 성공하고 최종 재고는 정확히 계산된다")
    void concurrentIncrease_shouldSucceedAllAndCalculateCorrectly() throws InterruptedException {
        // given
        int initialStock = 100;
        int increaseQuantity = 20;
        int threadCount = 30;

        Item item = Item.builder()
                .name("Test Item 2")
                .price(20000)
                .stock(initialStock)
                .build();
        Item savedItem = itemRepository.save(item);
        Long itemId = savedItem.getItemId();

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        executeConcurrently(threadCount, (index) -> {
            try {
                itemStockService.increaseStockForItem(itemId, increaseQuantity);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
                System.err.println("재고 증가 실패: " + e.getMessage());
            }
        });

        // then
        Item result = itemRepository.findById(itemId).orElseThrow();
        int expectedStock = initialStock + (increaseQuantity * threadCount);

        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(failCount.get()).isZero();
        assertThat(result.getStock()).isEqualTo(expectedStock);

        printResult("재고 증가", threadCount, successCount.get(), failCount.get(), expectedStock, result.getStock());
    }

    @Test
    @DisplayName("동일한 상품에 대해 재고 차감과 증가를 동시에 시도하면, 모두 성공하고 최종 재고는 정확히 계산된다")
    void concurrentDecreaseAndIncrease_shouldSucceedAllAndCalculateCorrectly() throws InterruptedException {
        // given
        int initialStock = 500;
        int decreaseQuantity = 10;
        int increaseQuantity = 15;
        int decreaseThreadCount = 20;
        int increaseThreadCount = 15;
        int totalThreadCount = decreaseThreadCount + increaseThreadCount;

        Item item = Item.builder()
                .name("Test Item 3")
                .price(30000)
                .stock(initialStock)
                .build();
        Item savedItem = itemRepository.save(item);
        Long itemId = savedItem.getItemId();

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        ExecutorService executor = Executors.newFixedThreadPool(totalThreadCount);
        CyclicBarrier barrier = new CyclicBarrier(totalThreadCount);
        CountDownLatch latch = new CountDownLatch(totalThreadCount);

        try {
            // 재고 차감 스레드
            for (int i = 0; i < decreaseThreadCount; i++) {
                executor.submit(() -> {
                    try {
                        barrier.await();
                        itemStockService.decreaseStockForItem(itemId, decreaseQuantity);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                        System.err.println("재고 차감 실패: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // 재고 증가 스레드
            for (int i = 0; i < increaseThreadCount; i++) {
                executor.submit(() -> {
                    try {
                        barrier.await();
                        itemStockService.increaseStockForItem(itemId, increaseQuantity);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                        System.err.println("재고 증가 실패: " + e.getMessage());
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
        Item result = itemRepository.findById(itemId).orElseThrow();
        int expectedStock = initialStock - (decreaseQuantity * decreaseThreadCount) + (increaseQuantity * increaseThreadCount);

        assertThat(successCount.get()).isEqualTo(totalThreadCount);
        assertThat(failCount.get()).isZero();
        assertThat(result.getStock()).isEqualTo(expectedStock);

        printResult("재고 차감/증가 혼합", totalThreadCount, successCount.get(), failCount.get(), expectedStock, result.getStock());
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

    private void printResult(String testName, int totalAttempts, int successCount, int failCount, int expectedStock, int actualStock) {
        System.out.println("=".repeat(60));
        System.out.println(testName + " 동시성 테스트 결과");
        System.out.println("=".repeat(60));
        System.out.println(String.format("총 시도:       %d", totalAttempts));
        System.out.println(String.format("성공:          %d", successCount));
        System.out.println(String.format("실패:          %d", failCount));
        System.out.println(String.format("예상 재고:     %d", expectedStock));
        System.out.println(String.format("실제 재고:     %d", actualStock));
        System.out.println(String.format("정확성:        %s", expectedStock == actualStock ? "✓" : "✗"));
        System.out.println("=".repeat(60));
    }

    @FunctionalInterface
    interface TaskWithIndex {
        void run(int index);
    }
}
