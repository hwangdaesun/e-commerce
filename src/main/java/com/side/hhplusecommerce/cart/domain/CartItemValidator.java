package com.side.hhplusecommerce.cart.domain;

import com.side.hhplusecommerce.cart.repository.CartItemRepository;
import com.side.hhplusecommerce.cart.repository.CartRepository;
import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartItemValidator {
    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;

    public CartItem validateOwnership(Long userId, Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_ITEM_NOT_FOUND));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_ITEM_NOT_FOUND));

        if (!cartItem.getCartId().equals(cart.getCartId())) {
            throw new CustomException(ErrorCode.NOT_OWNER_OF_CART_ITEM);
        }
        return cartItem;
    }

    public List<CartItem> validateOwnership(Long userId, List<Long> cartItemIds) {
        List<CartItem> cartItems = cartItemRepository.findByIdIn(cartItemIds);

        if (cartItems.size() != cartItemIds.size()) {
            throw new CustomException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_ITEM_NOT_FOUND));

        boolean hasInvalidCartItem = cartItems.stream()
                .anyMatch(cartItem -> !cartItem.getCartId().equals(cart.getCartId()));

        if (hasInvalidCartItem) {
            throw new CustomException(ErrorCode.NOT_OWNER_OF_CART_ITEM);
        }
        return cartItems;
    }
}
