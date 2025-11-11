package com.side.hhplusecommerce.cart.repository;

import com.side.hhplusecommerce.cart.domain.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCartId(Long cartId);
    Optional<CartItem> findById(Long cartItemId);
    List<CartItem> findByIdIn(List<Long> cartItemIds);
    void deleteByCartId(Long cartId);
}
