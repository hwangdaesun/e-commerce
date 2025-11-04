package com.side.hhplusecommerce.user.domain;

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
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("수량은 1개 이상이어야 합니다.");
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
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("수량은 1개 이상이어야 합니다.");
    }
}