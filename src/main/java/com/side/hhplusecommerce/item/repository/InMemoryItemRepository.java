package com.side.hhplusecommerce.item.repository;

import com.side.hhplusecommerce.item.domain.Item;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class InMemoryItemRepository implements ItemRepository {
    private final Map<Long, Item> store = new HashMap<>();

    @Override
    public Optional<Item> findById(Long itemId) {
        return Optional.ofNullable(store.get(itemId));
    }
}