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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.util.StopWatch;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemPopularityService {
    private static final int VIEW_COUNT_WEIGHT = 1;
    private static final int SALES_COUNT_WEIGHT = 10;
    private static final int BATCH_SIZE = 1000;

    private final ItemRepository itemRepository;
    private final ItemViewRepository itemViewRepository;
    private final OrderItemRepository orderItemRepository;
    private final ItemPopularityStatsRepository itemPopularityStatsRepository;
    private final JdbcTemplate jdbcTemplate;

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

    /**
     * 상품 인기도 통계를 bulk insert로 저장
     *
     * @param viewCountMap 상품별 조회수 맵
     * @param salesCountMap 상품별 판매량 맵
     * @param itemIds 모든 상품 ID 집합
     * @param basedOnDate 통계 기준 날짜
     * @return 저장된 총 레코드 수
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int bulkInsertPopularityStats(
            Map<Long, Long> viewCountMap,
            Map<Long, Long> salesCountMap,
            Iterable<Long> itemIds,
            LocalDateTime basedOnDate
    ) {
        // 각 상품별 통계 데이터 생성
        List<Object[]> batchArgs = new ArrayList<>();
        for (Long itemId : itemIds) {
            long viewCount = viewCountMap.getOrDefault(itemId, 0L);
            long salesCount = salesCountMap.getOrDefault(itemId, 0L);
            long popularityScore = viewCount + (salesCount * SALES_COUNT_WEIGHT);

            batchArgs.add(new Object[]{
                    itemId,
                    viewCount,
                    salesCount,
                    popularityScore,
                    Timestamp.valueOf(basedOnDate),
                    Timestamp.valueOf(LocalDateTime.now()),
                    Timestamp.valueOf(LocalDateTime.now())
            });
        }

        // JdbcTemplate을 사용한 청크 단위 bulk insert
        String sql = "INSERT INTO item_popularity_stats " +
                "(item_id, view_count, sales_count, popularity_score, based_on_date, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        int totalSaved = 0;
        for (int i = 0; i < batchArgs.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, batchArgs.size());
            List<Object[]> chunk = batchArgs.subList(i, end);

            int[] updateCounts = jdbcTemplate.batchUpdate(sql, chunk);
            totalSaved += updateCounts.length;

            log.debug("Saved chunk {}/{}: {} items",
                    (i / BATCH_SIZE) + 1,
                    (batchArgs.size() + BATCH_SIZE - 1) / BATCH_SIZE,
                    updateCounts.length);
        }

        log.info("Popularity stats bulk insert completed. Saved {} items in {} batches",
                totalSaved, (batchArgs.size() + BATCH_SIZE - 1) / BATCH_SIZE);

        return totalSaved;
    }

    private record ItemPopularity(Long itemId, Long popularityScore, Long viewCount, Long salesCount) {}
}
