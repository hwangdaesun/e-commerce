package com.side.hhplusecommerce.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.side.hhplusecommerce.order.domain.Order;
import com.side.hhplusecommerce.order.domain.OrderStatus;
import com.side.hhplusecommerce.order.repository.OrderRepository;
import com.side.hhplusecommerce.payment.service.UserPointService;
import com.side.hhplusecommerce.point.exception.InsufficientPointException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderPaymentServiceTest {

    @Mock
    private UserPointService userPointService;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderPaymentService orderPaymentService;

    @Test
    @DisplayName("포인트 차감이 성공하면 주문 상태가 PAID로 변경된다")
    void processOrderPayment_success() {
        // given
        Long userId = 1L;
        Order order = Order.create(1L, userId, 10000, 1000);

        doNothing().when(userPointService).use(userId, order.getFinalAmount());

        // when
        orderPaymentService.processOrderPayment(userId, order);

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(userPointService).use(userId, order.getFinalAmount());
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("포인트가 부족하면 예외를 발생시킨다")
    void processOrderPayment_fail_insufficient_point() {
        // given
        Long userId = 1L;
        Order order = Order.create(1L, userId, 10000, 1000);

        doThrow(new InsufficientPointException())
                .when(userPointService).use(userId, order.getFinalAmount());

        // when & then
        assertThatThrownBy(() -> orderPaymentService.processOrderPayment(userId, order))
                .isInstanceOf(InsufficientPointException.class);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("결제 금액이 0원인 경우에도 정상적으로 처리된다")
    void processOrderPayment_success_zero_amount() {
        // given
        Long userId = 1L;
        Integer totalAmount = 10000;
        Integer couponDiscount = 10000;
        Order order = Order.create(1L, userId, totalAmount, couponDiscount);

        doNothing().when(userPointService).use(userId, 0);

        // when
        orderPaymentService.processOrderPayment(userId, order);

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getFinalAmount()).isZero();
    }
}
