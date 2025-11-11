package com.side.hhplusecommerce.cart.repository;

import com.side.hhplusecommerce.cart.domain.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(Long userId);
    Cart save(Cart cart);
    void deleteAll();
}
