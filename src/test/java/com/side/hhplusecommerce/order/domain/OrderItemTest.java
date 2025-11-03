package com.side.hhplusecommerce.order.domain;

import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.order.exception.InvalidOrderItemItemIdException;
import com.side.hhplusecommerce.order.exception.InvalidOrderItemOrderIdException;
import com.side.hhplusecommerce.order.exception.InvalidOrderItemPriceException;
import com.side.hhplusecommerce.order.exception.InvalidOrderItemQuantityException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

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

    @Test
    @DisplayName("수량이 null인 경우 예외를 발생시킨다")
    void create_fail_null_quantity() {
        // given
        Long orderId = 1L;
        Long itemId = 100L;
        String name = "상품명";
        Integer price = 10000;
        Integer quantity = null;
        Long userCouponId = null;

        // when & then
        assertThatThrownBy(() -> OrderItem.create(orderId, itemId, name, price, quantity, userCouponId))
                .isInstanceOf(InvalidOrderItemQuantityException.class)
                .hasMessage(ErrorCode.INVALID_ORDER_ITEM_QUANTITY.getMessage());
    }

    @Test
    @DisplayName("가격이 null인 경우 예외를 발생시킨다")
    void create_fail_null_price() {
        // given
        Long orderId = 1L;
        Long itemId = 100L;
        String name = "상품명";
        Integer price = null;
        Integer quantity = 2;
        Long userCouponId = null;

        // when & then
        assertThatThrownBy(() -> OrderItem.create(orderId, itemId, name, price, quantity, userCouponId))
                .isInstanceOf(InvalidOrderItemPriceException.class)
                .hasMessage(ErrorCode.INVALID_ORDER_ITEM_PRICE.getMessage());
    }


    @ParameterizedTest
    @ValueSource(ints = {0, -1, -10, -100})
    @DisplayName("수량이 0 또는 음수인 경우 예외를 발생시킨다")
    void create_fail_invalid_quantity(int invalidQuantity) {
        // given
        Long orderId = 1L;
        Long itemId = 100L;
        String name = "상품명";
        Integer price = 10000;
        Long userCouponId = null;

        // when & then
        assertThatThrownBy(() -> OrderItem.create(orderId, itemId, name, price, invalidQuantity, userCouponId))
                .isInstanceOf(InvalidOrderItemQuantityException.class)
                .hasMessage(ErrorCode.INVALID_ORDER_ITEM_QUANTITY.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -10, -100, -1000})
    @DisplayName("가격이 음수인 경우 예외를 발생시킨다")
    void create_fail_negative_price(int negativePrice) {
        // given
        Long orderId = 1L;
        Long itemId = 100L;
        String name = "상품명";
        Integer quantity = 2;
        Long userCouponId = null;

        // when & then
        assertThatThrownBy(() -> OrderItem.create(orderId, itemId, name, negativePrice, quantity, userCouponId))
                .isInstanceOf(InvalidOrderItemPriceException.class)
                .hasMessage(ErrorCode.INVALID_ORDER_ITEM_PRICE.getMessage());
    }

    @Test
    @DisplayName("항목별 총액을 정확하게 계산한다")
    void calculateItemTotalPrice_success() {
        // given
        Long orderId = 1L;
        Long itemId = 100L;
        String name = "상품명";
        Integer price = 10000;
        Integer quantity = 3;
        Long userCouponId = null;

        OrderItem orderItem = OrderItem.create(orderId, itemId, name, price, quantity, userCouponId);

        // when
        Integer totalPrice = orderItem.calculateItemTotalPrice();

        // then
        assertThat(totalPrice).isEqualTo(30000);
    }

    @Test
    @DisplayName("쿠폰이 적용된 경우 true를 반환한다")
    void hasCoupon_true() {
        // given
        Long orderId = 1L;
        Long itemId = 100L;
        String name = "상품명";
        Integer price = 10000;
        Integer quantity = 2;
        Long userCouponId = 5L;

        OrderItem orderItem = OrderItem.create(orderId, itemId, name, price, quantity, userCouponId);

        // when
        boolean hasCoupon = orderItem.hasCoupon();

        // then
        assertThat(hasCoupon).isTrue();
    }

    @Test
    @DisplayName("쿠폰이 적용되지 않은 경우 false를 반환한다")
    void hasCoupon_false() {
        // given
        Long orderId = 1L;
        Long itemId = 100L;
        String name = "상품명";
        Integer price = 10000;
        Integer quantity = 2;
        Long userCouponId = null;

        OrderItem orderItem = OrderItem.create(orderId, itemId, name, price, quantity, userCouponId);

        // when
        boolean hasCoupon = orderItem.hasCoupon();

        // then
        assertThat(hasCoupon).isFalse();
    }

}
