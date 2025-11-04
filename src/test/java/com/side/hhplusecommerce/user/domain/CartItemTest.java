package com.side.hhplusecommerce.user.domain;

import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.user.exception.InvalidCartItemQuantityException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CartItemTest {

    @Test
    @DisplayName("장바구니 항목을 생성한다")
    void create_success() {
        // given
        Long cartId = 1L;
        Long itemId = 1L;
        Integer quantity = 3;

        // when
        CartItem cartItem = CartItem.create(cartId, itemId, quantity);

        // then
        assertThat(cartItem.getCartId()).isEqualTo(cartId);
        assertThat(cartItem.getItemId()).isEqualTo(itemId);
        assertThat(cartItem.getQuantity()).isEqualTo(quantity);
    }

    @Test
    @DisplayName("수량이 0이면 장바구니 항목 생성에 실패한다")
    void create_fail_zero_quantity() {
        // given
        Long cartId = 1L;
        Long itemId = 1L;
        Integer quantity = 0;

        // when & then
        assertThatThrownBy(() -> CartItem.create(cartId, itemId, quantity))
                .isInstanceOf(InvalidCartItemQuantityException.class)
                .hasMessage(ErrorCode.INVALID_CART_ITEM_QUANTITY.getMessage());
    }

    @Test
    @DisplayName("수량이 음수면 장바구니 항목 생성에 실패한다")
    void create_fail_negative_quantity() {
        // given
        Long cartId = 1L;
        Long itemId = 1L;
        Integer quantity = -1;

        // when & then
        assertThatThrownBy(() -> CartItem.create(cartId, itemId, quantity))
                .isInstanceOf(InvalidCartItemQuantityException.class)
                .hasMessage(ErrorCode.INVALID_CART_ITEM_QUANTITY.getMessage());
    }

    @Test
    @DisplayName("장바구니 항목의 수량을 변경한다")
    void updateQuantity_success() {
        // given
        CartItem cartItem = CartItem.create(1L, 1L, 3);

        // when
        cartItem.updateQuantity(5);

        // then
        assertThat(cartItem.getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("수량을 0으로 변경하면 실패한다")
    void updateQuantity_fail_zero() {
        // given
        CartItem cartItem = CartItem.create(1L, 1L, 3);

        // when & then
        assertThatThrownBy(() -> cartItem.updateQuantity(0))
                .isInstanceOf(InvalidCartItemQuantityException.class)
                .hasMessage(ErrorCode.INVALID_CART_ITEM_QUANTITY.getMessage());
    }

    @Test
    @DisplayName("수량을 음수로 변경하면 실패한다")
    void updateQuantity_fail_negative() {
        // given
        CartItem cartItem = CartItem.create(1L, 1L, 3);

        // when & then
        assertThatThrownBy(() -> cartItem.updateQuantity(-1))
                .isInstanceOf(InvalidCartItemQuantityException.class)
                .hasMessage(ErrorCode.INVALID_CART_ITEM_QUANTITY.getMessage());
    }

    @Test
    @DisplayName("장바구니 항목의 총액을 계산한다")
    void calculateTotalPrice() {
        // given
        CartItem cartItem = CartItem.create(1L, 1L, 3);
        Integer itemPrice = 10000;

        // when
        Integer totalPrice = cartItem.calculateTotalPrice(itemPrice);

        // then
        assertThat(totalPrice).isEqualTo(30000);
    }

}
