package com.side.hhplusecommerce.order.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.side.hhplusecommerce.ContainerTest;
import com.side.hhplusecommerce.cart.domain.Cart;
import com.side.hhplusecommerce.cart.domain.CartItem;
import com.side.hhplusecommerce.cart.repository.CartRepository;
import com.side.hhplusecommerce.cart.repository.CartItemRepository;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.repository.ItemRepository;
import com.side.hhplusecommerce.order.repository.OrderRepository;
import com.side.hhplusecommerce.order.usecase.OrderCreateUseCase;
import com.side.hhplusecommerce.point.domain.UserPoint;
import com.side.hhplusecommerce.point.repository.UserPointRepository;
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
class OrderConcurrencyIntegrationTest extends ContainerTest {

    @Autowired
    private OrderCreateUseCase orderCreateUseCase;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserPointRepository userPointRepository;

    @DisplayName("특정 상품의 재고가 9개인데 동시에 20명이 주문을 시도하면, 재고가 0개가 되고, 주문은 9명 성공하고, 나머지는 실패한다")
    @Test
    void concurrentOrderCreation_shouldSucceedExactly9Orders() throws InterruptedException {
        // given
        int initialStock = 9;
        int threadCount = 20;
        int orderQuantity = 1;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // 상품 생성 (재고 9개)
        Item item = Item.builder()
                .name("테스트 상품")
                .price(10000)
                .stock(initialStock)
                .build();
        Item savedItem = itemRepository.save(item);

        // 10명의 사용자를 위한 UserPoint, Cart, CartItem 생성
        for (long userId = 1; userId <= threadCount; userId++) {
            // 사용자 포인트 생성 (충분한 포인트 충전)
            UserPoint userPoint = UserPoint.initialize(userId);
            userPoint.charge(100000); // 10만 포인트 충전
            userPointRepository.save(userPoint);

            // 장바구니 생성
            Cart cart = Cart.builder()
                    .userId(userId)
                    .build();
            Cart savedCart = cartRepository.save(cart);

            // 장바구니 아이템 추가
            CartItem cartItem = CartItem.create(
                    savedCart.getCartId(),
                    savedItem.getItemId(),
                    orderQuantity
            );
            cartItemRepository.save(cartItem);
        }

        // when
        executeConcurrently(threadCount, (int threadIndex) -> {
            Long userId = (long) (threadIndex + 1);
            try {
                // 해당 사용자의 장바구니 아이템 조회
                Cart cart = cartRepository.findByUserId(userId).orElseThrow();
                List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getCartId());
                List<Long> cartItemIds = cartItems.stream()
                        .map(CartItem::getCartItemId)
                        .toList();

                // 주문 생성 (쿠폰 없음)
                orderCreateUseCase.create(userId, cartItemIds, null);
                successCount.incrementAndGet();
            } catch (Exception e) {
                // 재고 부족 또는 PessimisticLock 타임아웃 시
                failCount.incrementAndGet();
                System.err.println("주문 실패 (userId=" + userId + "): " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        });

        // then
        int totalAttempts = successCount.get() + failCount.get();
        Item resultItem = itemRepository.findById(savedItem.getItemId()).orElseThrow();
        long orderCount = orderRepository.count();

        assertThat(totalAttempts).isEqualTo(threadCount); // 모든 시도가 처리됨
        assertThat(successCount.get()).isEqualTo(initialStock); // 정확히 재고 수만큼만 성공
        assertThat(failCount.get()).isEqualTo(threadCount - initialStock); // 나머지는 실패
        assertThat(resultItem.getStock()).isZero(); // 재고가 0이 됨
        assertThat(orderCount).isEqualTo(initialStock); // 주문이 재고 수만큼 생성됨

        printTestResult(totalAttempts, successCount.get(), failCount.get(), resultItem.getStock(), orderCount);
    }


    /**
     * 여러 스레드를 동시에 시작하여 작업을 실행하는 헬퍼 메서드
     *
     * @param threadCount 실행할 스레드 수
     * @param task 각 스레드에서 실행할 작업 (스레드 인덱스를 받음)
     */
    private void executeConcurrently(int threadCount, TaskWithIndex task) throws InterruptedException {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadCount);
        CyclicBarrier startBarrier = new CyclicBarrier(threadCount);
        CountDownLatch completeLatch = new CountDownLatch(threadCount);

        try {
            for (int i = 0; i < threadCount; i++) {
                final int threadIndex = i;
                executor.submit(() -> {
                    try {
                        startBarrier.await();   // 모든 스레드가 준비될 때까지 대기
                        task.run(threadIndex);  // 실제 작업 실행
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        completeLatch.countDown(); // 완료 신호
                    }
                });
            }

            // 모든 스레드가 작업을 완료할 때까지 대기 (최대 120초)
            boolean completed = completeLatch.await(120, TimeUnit.SECONDS);
            assertThat(completed).withFailMessage("테스트 타임아웃: 120초 내에 완료되지 않음").isTrue();
        } finally {
            executor.shutdown();
        }
    }

    /**
     * 테스트 결과를 포맷팅하여 출력하는 헬퍼 메서드
     */
    private void printTestResult(int totalAttempts, int successCount, int failCount, int finalStock, long orderCount) {
        System.out.println("=".repeat(60));
        System.out.println("주문 동시성 테스트 결과");
        System.out.println("=".repeat(60));
        System.out.println(String.format("총 시도:       %d", totalAttempts));
        System.out.println(String.format("성공:          %d", successCount));
        System.out.println(String.format("실패:          %d", failCount));
        System.out.println(String.format("최종 재고:     %d", finalStock));
        System.out.println(String.format("생성된 주문:   %d", orderCount));
        System.out.println("=".repeat(60));
    }

    @FunctionalInterface
    interface TaskWithIndex {
        void run(int threadIndex);
    }
}
