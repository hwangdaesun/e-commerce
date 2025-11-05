package com.side.hhplusecommerce.cart.domain;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class CartItemValidator {

    public void validateOwnership(CartItem cartItem, Cart cart) {
        if (!cartItem.getCartId().equals(cart.getCartId())) {
            throw new CustomException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
    }
}
