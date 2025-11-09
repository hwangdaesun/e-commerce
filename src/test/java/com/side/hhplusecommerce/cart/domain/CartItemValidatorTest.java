package com.side.hhplusecommerce.cart.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.side.hhplusecommerce.cart.repository.CartItemRepository;
import com.side.hhplusecommerce.cart.repository.CartRepository;
import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartItemValidatorTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private CartItemValidator cartItemValidator;

    @Test
    @DisplayName("단일 장바구니 항목 검증 시 본인 소유이면 항목을 반환한다")
    void validateOwnership_single_success() {
        // given
        Long userId = 1L;
        Long cartItemId = 1L;
        Long cartId = 10L;

        CartItem cartItem = CartItem.createWithId(cartItemId, cartId, 100L, 2);

        Cart cart = Cart.builder()
                .cartId(cartId)
                .userId(userId)
                .build();

        given(cartItemRepository.findById(cartItemId)).willReturn(Optional.of(cartItem));
        given(cartRepository.findByUserId(userId)).willReturn(Optional.of(cart));

        // when & then
        CartItem result = cartItemValidator.validateOwnership(userId, cartItemId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getCartItemId()).isEqualTo(cartItemId);
        assertThat(result.getCartId()).isEqualTo(cartId);
    }

    @Test
    @DisplayName("단일 장바구니 항목 검증 시 항목이 존재하지 않으면 예외를 발생시킨다")
    void validateOwnership_single_fail_item_not_found() {
        // given
        Long userId = 1L;
        Long cartItemId = 1L;

        given(cartItemRepository.findById(cartItemId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> cartItemValidator.validateOwnership(userId, cartItemId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.CART_ITEM_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("단일 장바구니 항목 검증 시 사용자의 장바구니가 존재하지 않으면 예외를 발생시킨다")
    void validateOwnership_single_fail_cart_not_found() {
        // given
        Long userId = 1L;
        Long cartItemId = 1L;

        CartItem cartItem = CartItem.createWithId(cartItemId, 10L, 100L, 2);

        given(cartItemRepository.findById(cartItemId)).willReturn(Optional.of(cartItem));
        given(cartRepository.findByUserId(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> cartItemValidator.validateOwnership(userId, cartItemId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.CART_ITEM_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("단일 장바구니 항목 검증 시 본인 소유가 아니면 예외를 발생시킨다")
    void validateOwnership_single_fail_not_owner() {
        // given
        Long userId = 1L;
        Long cartItemId = 1L;
        Long cartId = 10L;
        Long otherCartId = 20L;

        CartItem cartItem = CartItem.createWithId(cartItemId, otherCartId, 100L, 2);

        Cart cart = Cart.builder()
                .cartId(cartId)
                .userId(userId)
                .build();

        given(cartItemRepository.findById(cartItemId)).willReturn(Optional.of(cartItem));
        given(cartRepository.findByUserId(userId)).willReturn(Optional.of(cart));

        // when & then
        assertThatThrownBy(() -> cartItemValidator.validateOwnership(userId, cartItemId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.NOT_OWNER_OF_CART_ITEM.getMessage());
    }

    @Test
    @DisplayName("여러 장바구니 항목 검증 시 모두 본인 소유이면 항목 목록을 반환한다")
    void validateOwnership_multiple_success() {
        // given
        Long userId = 1L;
        Long cartId = 10L;
        List<Long> cartItemIds = List.of(1L, 2L, 3L);

        List<CartItem> cartItems = List.of(
                CartItem.createWithId(1L, cartId, 100L, 1),
                CartItem.createWithId(2L, cartId, 200L, 2),
                CartItem.createWithId(3L, cartId, 300L, 3)
        );

        Cart cart = Cart.builder()
                .cartId(cartId)
                .userId(userId)
                .build();

        given(cartItemRepository.findByIdIn(cartItemIds)).willReturn(cartItems);
        given(cartRepository.findByUserId(userId)).willReturn(Optional.of(cart));

        // when
        List<CartItem> result = cartItemValidator.validateOwnership(userId, cartItemIds);

        // then
        assertThat(result).hasSize(3);
        assertThat(result).extracting(CartItem::getCartItemId)
                .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    @DisplayName("여러 장바구니 항목 검증 시 일부만 존재하면 예외를 발생시킨다")
    void validateOwnership_multiple_fail_partial_not_found() {
        // given
        Long userId = 1L;
        List<Long> cartItemIds = List.of(1L, 2L, 3L);

        List<CartItem> cartItems = List.of(
                CartItem.createWithId(1L, 10L, 100L, 1),
                CartItem.createWithId(2L, 10L, 200L, 2)
        );

        given(cartItemRepository.findByIdIn(cartItemIds)).willReturn(cartItems);

        // when & then
        assertThatThrownBy(() -> cartItemValidator.validateOwnership(userId, cartItemIds))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.CART_ITEM_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("여러 장바구니 항목 검증 시 사용자의 장바구니가 존재하지 않으면 예외를 발생시킨다")
    void validateOwnership_multiple_fail_cart_not_found() {
        // given
        Long userId = 1L;
        List<Long> cartItemIds = List.of(1L, 2L, 3L);

        List<CartItem> cartItems = List.of(
                CartItem.createWithId(1L, 10L, 100L, 1),
                CartItem.createWithId(2L, 10L, 200L, 2),
                CartItem.createWithId(3L, 10L, 300L, 3)
        );

        given(cartItemRepository.findByIdIn(cartItemIds)).willReturn(cartItems);
        given(cartRepository.findByUserId(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> cartItemValidator.validateOwnership(userId, cartItemIds))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.CART_ITEM_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("여러 장바구니 항목 검증 시 일부가 본인 소유가 아니면 예외를 발생시킨다")
    void validateOwnership_multiple_fail_partial_not_owner() {
        // given
        Long userId = 1L;
        Long cartId = 10L;
        Long otherCartId = 20L;
        List<Long> cartItemIds = List.of(1L, 2L, 3L);

        List<CartItem> cartItems = List.of(
                CartItem.createWithId(1L, cartId, 100L, 1),
                CartItem.createWithId(2L, otherCartId, 200L, 2),
                CartItem.createWithId(3L, cartId, 300L, 3)
        );

        Cart cart = Cart.builder()
                .cartId(cartId)
                .userId(userId)
                .build();

        given(cartItemRepository.findByIdIn(cartItemIds)).willReturn(cartItems);
        given(cartRepository.findByUserId(userId)).willReturn(Optional.of(cart));

        // when & then
        assertThatThrownBy(() -> cartItemValidator.validateOwnership(userId, cartItemIds))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.NOT_OWNER_OF_CART_ITEM.getMessage());
    }
}
