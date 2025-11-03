package com.side.hhplusecommerce.order.domain;

import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.order.exception.InvalidCouponDiscountException;
import com.side.hhplusecommerce.order.exception.InvalidOrderAmountException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
