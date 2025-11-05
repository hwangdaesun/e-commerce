package com.side.hhplusecommerce.cart.domain;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CartItemValidator {

    public void validateOwnership(CartItem cartItem, Cart cart) {
        if (!cartItem.getCartId().equals(cart.getCartId())) {
            throw new CustomException(ErrorCode.NOT_OWNER_OF_CART_ITEM);
        }
    }

    public void validateOwnership(List<CartItem> cartItems, Cart cart) {
        boolean hasInvalidCartItem = cartItems.stream()
                .anyMatch(cartItem -> !cartItem.getCartId().equals(cart.getCartId()));

        if (hasInvalidCartItem) {
            throw new CustomException(ErrorCode.NOT_OWNER_OF_CART_ITEM);
        }
    }
}
