package com.side.hhplusecommerce.cart.repository;

import com.side.hhplusecommerce.cart.domain.Cart;

import java.util.Optional;

public interface CartRepository {
    Optional<Cart> findByUserId(Long userId);
    Cart save(Cart cart);
    void deleteAll();
}
