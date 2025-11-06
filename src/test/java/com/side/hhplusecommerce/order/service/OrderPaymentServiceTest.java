package com.side.hhplusecommerce.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.order.domain.Order;
import com.side.hhplusecommerce.order.domain.OrderStatus;
import com.side.hhplusecommerce.order.repository.OrderRepository;
import com.side.hhplusecommerce.payment.PaymentResult;
import com.side.hhplusecommerce.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderPaymentServiceTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderPaymentService orderPaymentService;

    @Test
    @DisplayName("결제가 성공하면 주문 상태가 PAID로 변경된다")
    void processOrderPayment_success() {
        // given
        Long userId = 1L;
        Order order = Order.create(1L, userId, 10000, 1000);

        given(paymentService.processPayment(userId, order.getOrderId(), order.getFinalAmount()))
                .willReturn(PaymentResult.success());

        // when
        orderPaymentService.processOrderPayment(userId, order);

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(paymentService).processPayment(userId, order.getOrderId(), order.getFinalAmount());
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("결제가 실패하면 예외를 발생시킨다")
    void processOrderPayment_fail_payment() {
        // given
        Long userId = 1L;
        Order order = Order.create(1L, userId, 10000, 1000);

        given(paymentService.processPayment(anyLong(), anyLong(), anyInt()))
                .willReturn(PaymentResult.failure("포인트 부족"));

        // when & then
        assertThatThrownBy(() -> orderPaymentService.processOrderPayment(userId, order))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INSUFFICIENT_POINT.getMessage());

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

        given(paymentService.processPayment(userId, order.getOrderId(), 0))
                .willReturn(PaymentResult.success());

        // when
        orderPaymentService.processOrderPayment(userId, order);

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getFinalAmount()).isZero();
    }
}
