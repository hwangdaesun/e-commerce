package com.side.hhplusecommerce.user.repository;

import com.side.hhplusecommerce.user.domain.CartItem;

public interface CartItemRepository {
    CartItem save(CartItem cartItem);
}
