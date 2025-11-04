package com.side.hhplusecommerce.order.domain;

import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.order.exception.InvalidCouponDiscountException;
import com.side.hhplusecommerce.order.exception.InvalidOrderAmountException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

class OrderTest {

    @Test
    @DisplayName("총 주문 금액이 음수인 경우 예외를 발생시킨다")
    void create_fail_negative_total_amount() {
        // given
        Long orderId = 1L;
        Long userId = 1L;
        Integer totalAmount = -1000;
        Integer couponDiscount = 0;

        // when & then
        assertThatThrownBy(() -> Order.create(orderId, userId, totalAmount, couponDiscount))
                .isInstanceOf(InvalidOrderAmountException.class)
                .hasMessage(ErrorCode.INVALID_ORDER_AMOUNT.getMessage());
    }

    @Test
    @DisplayName("쿠폰 할인 금액이 음수인 경우 예외를 발생시킨다")
    void create_fail_negative_coupon_discount() {
        // given
        Long orderId = 1L;
        Long userId = 1L;
        Integer totalAmount = 10000;
        Integer couponDiscount = -1000;

        // when & then
        assertThatThrownBy(() -> Order.create(orderId, userId, totalAmount, couponDiscount))
                .isInstanceOf(InvalidCouponDiscountException.class)
                .hasMessage(ErrorCode.INVALID_COUPON_DISCOUNT.getMessage());
    }

    @ParameterizedTest(name = "총액={0}, 할인={1} -> 최종금액={2}")
    @CsvSource({
            "10000, 0, 10000",      // 쿠폰 할인 없음
            "10000, 2000, 8000",    // 정상 할인 적용
            "5000, 10000, 0",       // 할인 금액이 총액보다 큼
            "10000, 10000, 0"       // 할인 금액과 총액이 같음
    })
    @DisplayName("주문 최종 금액이 올바르게 계산된다")
    void calculate_final_amount(Integer totalAmount, Integer couponDiscount, Integer expectedFinalAmount) {
        // given
        Long orderId = 1L;
        Long userId = 1L;

        // when
        Order order = Order.create(orderId, userId, totalAmount, couponDiscount);

        // then
        assertThat(order.getFinalAmount()).isEqualTo(expectedFinalAmount);
        assertThat(order.getTotalAmount()).isEqualTo(totalAmount);
        assertThat(order.getCouponDiscount()).isEqualTo(couponDiscount);
    }

    @Test
    @DisplayName("PENDING 상태의 주문을 결제 완료 처리할 수 있다")
    void complete_pay_from_pending() {
        // given
        Order order = Order.create(1L, 1L, 10000, 0);

        // when
        order.completePay();

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

}
