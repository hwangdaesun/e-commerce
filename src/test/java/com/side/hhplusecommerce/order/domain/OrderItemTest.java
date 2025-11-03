package com.side.hhplusecommerce.order.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.order.exception.InvalidOrderItemItemIdException;
import com.side.hhplusecommerce.order.exception.InvalidOrderItemOrderIdException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderItemTest {

    @Test
    @DisplayName("orderId가 null인 경우 예외를 발생시킨다")
    void create_fail_null_orderId() {
        // given
        Long orderId = null;
        Long itemId = 100L;
        String name = "상품명";
        Integer price = 10000;
        Integer quantity = 2;
        Long userCouponId = null;

        // when & then
        assertThatThrownBy(() -> OrderItem.create(orderId, itemId, name, price, quantity, userCouponId))
                .isInstanceOf(InvalidOrderItemOrderIdException.class)
                .hasMessage(ErrorCode.INVALID_ORDER_ITEM_ORDER_ID.getMessage());
    }

    @Test
    @DisplayName("itemId가 null인 경우 예외를 발생시킨다")
    void create_fail_null_itemId() {
        // given
        Long orderId = 1L;
        Long itemId = null;
        String name = "상품명";
        Integer price = 10000;
        Integer quantity = 2;
        Long userCouponId = null;

        // when & then
        assertThatThrownBy(() -> OrderItem.create(orderId, itemId, name, price, quantity, userCouponId))
                .isInstanceOf(InvalidOrderItemItemIdException.class)
                .hasMessage(ErrorCode.INVALID_ORDER_ITEM_ITEM_ID.getMessage());
    }

}
