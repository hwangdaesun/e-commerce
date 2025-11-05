package com.side.hhplusecommerce.cart.repository;

import com.side.hhplusecommerce.cart.domain.CartItem;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository {
    CartItem save(CartItem cartItem);
    List<CartItem> findByCartId(Long cartId);
    Optional<CartItem> findById(Long cartItemId);
}
