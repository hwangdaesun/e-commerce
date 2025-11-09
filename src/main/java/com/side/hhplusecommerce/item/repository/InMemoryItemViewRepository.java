package com.side.hhplusecommerce.item.repository;

import com.side.hhplusecommerce.item.domain.ItemView;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryItemViewRepository implements ItemViewRepository {
    private final Map<Long, ItemView> store = new ConcurrentHashMap<>();

    @Override
    public Map<Long, Long> countByItemIdsAndCreatedAtAfter(List<Long> itemIds, LocalDateTime after) {
        return store.values().stream()
                .filter(itemView -> itemIds.contains(itemView.getItemId()))
                .filter(itemView -> itemView.getCreatedAt().isAfter(after))
                .collect(Collectors.groupingBy(ItemView::getItemId, Collectors.counting()));
    }
}
