package com.side.hhplusecommerce.cart.service;

import com.side.hhplusecommerce.cart.domain.CartItem;
import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.item.domain.Item;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class CartItemServiceTest {

    private final CartItemService cartItemService = new CartItemService();

    @Test
    @DisplayName("장바구니 항목들의 총 금액을 계산한다")
    void calculateTotalAmount_success() {
        // given
        Integer item1Price = 10000;
        Integer item1Quantity = 2;
        Integer item2Price = 20000;
        Integer item2Quantity = 3;

        List<CartItem> cartItems = List.of(
                CartItem.createWithId(1L, 100L, 1L, item1Quantity),
                CartItem.createWithId(2L, 100L, 2L, item2Quantity)
        );

        List<Item> items = List.of(
                Item.builder().itemId(1L).name("Item 1").price(item1Price).stock(100).build(),
                Item.builder().itemId(2L).name("Item 2").price(item2Price).stock(100).build()
        );

        Integer expectedTotal = (item1Price * item1Quantity) + (item2Price * item2Quantity);

        // when
        Integer totalAmount = cartItemService.calculateTotalAmount(cartItems, items);

        // then
        assertThat(totalAmount).isEqualTo(expectedTotal);
    }

    @Test
    @DisplayName("장바구니 항목에 해당하는 상품이 없으면 예외를 발생시킨다")
    void calculateTotalAmount_fail_item_not_found() {
        // given
        List<CartItem> cartItems = List.of(
                CartItem.createWithId(1L, 100L, 1L, 2),
                CartItem.createWithId(2L, 100L, 2L, 3)
        );

        List<Item> items = List.of(
                Item.builder().itemId(1L).name("Item 1").price(10000).stock(100).build()
                // itemId 2가 없음
        );

        // when & then
        assertThatThrownBy(() -> cartItemService.calculateTotalAmount(cartItems, items))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.ITEM_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("장바구니가 비어있으면 0원을 반환한다")
    void calculateTotalAmount_success_empty_cart() {
        // given
        List<CartItem> cartItems = List.of();
        List<Item> items = List.of();

        // when
        Integer totalAmount = cartItemService.calculateTotalAmount(cartItems, items);

        // then
        assertThat(totalAmount).isEqualTo(0);
    }

    @Test
    @DisplayName("가격이 0원인 상품도 정상적으로 계산한다")
    void calculateTotalAmount_success_zero_price() {
        // given
        List<CartItem> cartItems = List.of(
                CartItem.createWithId(1L, 100L, 1L, 5)
        );

        List<Item> items = List.of(
                Item.builder().itemId(1L).name("Free Item").price(0).stock(100).build()
        );

        // when
        Integer totalAmount = cartItemService.calculateTotalAmount(cartItems, items);

        // then
        assertThat(totalAmount).isEqualTo(0);
    }

}
