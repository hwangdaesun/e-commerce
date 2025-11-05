package com.side.hhplusecommerce.item.repository;

import com.side.hhplusecommerce.item.domain.Item;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryItemRepository implements ItemRepository {
    private final Map<Long, Item> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

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

    @Override
    public List<Item> findPopularItems(Integer limit, LocalDateTime after) {
        return store.values().stream().toList();
    }

    @Override
    public List<Item> findAllByIds(List<Long> itemIds) {
        return itemIds.stream()
                .map(store::get)
                .filter(item -> item != null)
                .collect(Collectors.toList());
    }

    @Override
    public void save(Item item) {
        if (Objects.isNull(item.getItemId())) {
            Long id = idGenerator.getAndIncrement();
            Item newItem = Item.builder()
                    .itemId(id)
                    .name(item.getName())
                    .price(item.getPrice())
                    .stock(item.getStock())
                    .salesCount(item.getSalesCount())
                    .build();
            store.put(id, newItem);
        } else {
            store.put(item.getItemId(), item);
        }
    }
}
