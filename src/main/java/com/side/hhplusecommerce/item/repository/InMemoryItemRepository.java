package com.side.hhplusecommerce.item.repository;

import com.side.hhplusecommerce.item.domain.Item;
import org.springframework.stereotype.Repository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryItemRepository implements ItemRepository {
    private final Map<Long, Item> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Item> findById(Long itemId) {
        return Optional.ofNullable(store.get(itemId));
    }
}