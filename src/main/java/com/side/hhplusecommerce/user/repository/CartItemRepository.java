package com.side.hhplusecommerce.user.repository;

import com.side.hhplusecommerce.user.domain.CartItem;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository {
    CartItem save(CartItem cartItem);
    List<CartItem> findByCartId(Long cartId);
    Optional<CartItem> findById(Long cartItemId);
}
