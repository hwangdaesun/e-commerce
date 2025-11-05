package com.side.hhplusecommerce.cart.repository;

import com.side.hhplusecommerce.cart.domain.CartItem;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryCartItemRepository implements CartItemRepository {
    private final Map<Long, CartItem> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public CartItem save(CartItem cartItem) {
        if (Objects.isNull(cartItem.getCartItemId())) {
            Long id = idGenerator.getAndIncrement();
            CartItem newCartItem = CartItem.createWithId(
                    id,
                    cartItem.getCartId(),
                    cartItem.getItemId(),
                    cartItem.getQuantity()
            );
            store.put(id, newCartItem);
            return newCartItem;
        }
        store.put(cartItem.getCartItemId(), cartItem);
        return cartItem;
    }

    @Override
    public List<CartItem> findByCartId(Long cartId) {
        return store.values().stream()
                .filter(cartItem -> cartItem.getCartId().equals(cartId))
                .toList();
    }

    @Override
    public Optional<CartItem> findById(Long cartItemId) {
        return Optional.ofNullable(store.get(cartItemId));
    }

    @Override
    public List<CartItem> findByIdIn(List<Long> cartItemIds) {
        return cartItemIds.stream()
                .map(store::get)
                .filter(Objects::nonNull)
                .toList();
    }
}
