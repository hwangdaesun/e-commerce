package com.side.hhplusecommerce.item.service;

import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.domain.ItemPopularityStats;
import com.side.hhplusecommerce.item.dto.ItemViewCountDto;
import com.side.hhplusecommerce.item.repository.ItemPopularityStatsRepository;
import com.side.hhplusecommerce.item.repository.ItemRepository;
import com.side.hhplusecommerce.item.repository.ItemViewRepository;
import com.side.hhplusecommerce.order.dto.ItemSalesCountDto;
import com.side.hhplusecommerce.order.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.util.StopWatch;

@Service
@RequiredArgsConstructor
public class ItemPopularityService {
    private static final int VIEW_COUNT_WEIGHT = 1;
    private static final int SALES_COUNT_WEIGHT = 10;

    private final ItemRepository itemRepository;
    private final ItemViewRepository itemViewRepository;
    private final OrderItemRepository orderItemRepository;
    private final ItemPopularityStatsRepository itemPopularityStatsRepository;

    @Transactional(readOnly = true)
    public List<Item> getPopularItemsV1(LocalDateTime after, int limit) {
        // 조회수 집계
        Map<Long, Long> viewCountMap = itemViewRepository.countViewsByItemIdGrouped(after)
                .stream()
                .collect(Collectors.toMap(
                        ItemViewCountDto::getItemId,
                        ItemViewCountDto::getViewCount
                ));

        // 판매량 집계
        Map<Long, Long> salesCountMap = orderItemRepository.countSalesByItemIdGrouped(after)
                .stream()
                .collect(Collectors.toMap(
                        ItemSalesCountDto::getItemId,
                        ItemSalesCountDto::getSalesCount
                ));

        // 모든 itemId 수집
        Set<Long> itemIds = new HashSet<>();
        itemIds.addAll(viewCountMap.keySet());
        itemIds.addAll(salesCountMap.keySet());

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        // 인기도 계산 및 정렬
        List<Long> popularItemIds = itemIds.stream()
                .map(itemId -> {
                    long viewCount = viewCountMap.getOrDefault(itemId, 0L);
                    long salesCount = salesCountMap.getOrDefault(itemId, 0L);
                    long popularityScore = viewCount * VIEW_COUNT_WEIGHT + salesCount * SALES_COUNT_WEIGHT;
                    return new ItemPopularity(itemId, popularityScore, viewCount, salesCount);
                })
                .sorted(Comparator.comparing(ItemPopularity::popularityScore).reversed())
                .limit(limit)
                .map(itemPopularity -> itemPopularity.itemId)
                .toList();
        // todo : 병렬 처리를 하면 어떻게 될까?
        stopWatch.stop();
        System.out.println(stopWatch.prettyPrint());

        return itemRepository.findAllByItemIdIn(popularItemIds);
    }

    @Transactional(readOnly = true)
    public List<Item> getPopularItemsV2(int limit) {
        List<ItemPopularityStats> topStats = itemPopularityStatsRepository
                .findTopByLatestBasedOnDate(PageRequest.of(0, limit));

        List<Long> itemIds = topStats.stream()
                .map(ItemPopularityStats::getItemId)
                .toList();

        return itemRepository.findAllByItemIdIn(itemIds).stream()
                .sorted(Comparator.comparing(item -> itemIds.indexOf(item.getItemId())))
                .toList();
    }

    private record ItemPopularity(Long itemId, Long popularityScore, Long viewCount, Long salesCount) {}
}
