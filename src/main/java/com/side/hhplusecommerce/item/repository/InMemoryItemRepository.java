package com.side.hhplusecommerce.item.repository;

import com.side.hhplusecommerce.item.domain.Item;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryItemRepository implements ItemRepository {
    private final Map<Long, Item> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Item> findById(Long itemId) {
        return Optional.ofNullable(store.get(itemId));
    }

    @Override
    public List<Item> findAllWithCursor(Long cursor, Integer size) {
        return store.values().stream()
                .sorted(Comparator.comparing(Item::getCreatedAt).reversed()
                        .thenComparing(Comparator.comparing(Item::getItemId).reversed()))
                .filter(item -> cursor == null || item.getItemId() < cursor)
                .limit(size + 1)
                .collect(Collectors.toList());
    }
}