package com.side.hhplusecommerce.item.service;

import com.side.hhplusecommerce.item.domain.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemPopularityService {
    private static final int VIEW_COUNT_WEIGHT = 9;
    private static final int SALES_COUNT_WEIGHT = 1;

    public List<Long> getPopularItemIds(List<Item> items, Map<Long, Long> viewCountMap, Integer limit) {
        return items.stream()
                .map(item -> {
                    Long viewCount = viewCountMap.getOrDefault(item.getItemId(), 0L);
                    Long popularityScore = calculatePopularity(viewCount, item.getSalesCount());
                    return new ItemWithPopularity(item.getItemId(), popularityScore);
                })
                .sorted(Comparator.comparing(ItemWithPopularity::popularityScore).reversed())
                .limit(limit)
                .map(ItemWithPopularity::itemId)
                .collect(Collectors.toList());
    }

    private Long calculatePopularity(Long viewCount, Integer salesCount) {
        return viewCount * VIEW_COUNT_WEIGHT + salesCount * SALES_COUNT_WEIGHT;
    }
}