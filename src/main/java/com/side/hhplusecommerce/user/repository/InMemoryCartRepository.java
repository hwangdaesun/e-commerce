package com.side.hhplusecommerce.user.repository;

import com.side.hhplusecommerce.user.domain.Cart;
import java.util.Objects;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryCartRepository implements CartRepository {
    private final Map<Long, Cart> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Optional<Cart> findByUserId(Long userId) {
        return store.values().stream()
                .filter(cart -> cart.getUserId().equals(userId))
                .findFirst();
    }

    @Override
    public Cart save(Cart cart) {
        if (Objects.isNull(cart.getCartId())) {
            Long id = idGenerator.getAndIncrement();
            Cart newCart = Cart.builder()
                    .cartId(id)
                    .userId(cart.getUserId())
                    .build();
            store.put(id, newCart);
            return newCart;
        }
        store.put(cart.getCartId(), cart);
        return cart;
    }
}