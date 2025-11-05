package com.side.hhplusecommerce.user.repository;

import com.side.hhplusecommerce.user.domain.CartItem;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

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
}
