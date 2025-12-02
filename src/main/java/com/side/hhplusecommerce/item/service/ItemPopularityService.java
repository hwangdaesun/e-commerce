package com.side.hhplusecommerce.item.service;

import static com.side.hhplusecommerce.config.RedisCacheConfig.ITEM;
import static com.side.hhplusecommerce.config.RedisCacheConfig.LOW_STOCK_THRESHOLD;
import static com.side.hhplusecommerce.config.RedisCacheConfig.POPULAR_ITEMS;
import static com.side.hhplusecommerce.config.RedisCacheConfig.TEMP_SUFFIX;
import static com.side.hhplusecommerce.item.constants.PopularityConstants.*;
import static java.time.Duration.*;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.item.constants.PopularityConstants;
import com.side.hhplusecommerce.item.constants.PopularityPeriod;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.dto.ItemDto;
import com.side.hhplusecommerce.item.dto.ItemViewCountDto;
import com.side.hhplusecommerce.item.dto.PopularItemDto;
import com.side.hhplusecommerce.item.dto.PopularItemsDto;
import com.side.hhplusecommerce.item.repository.ItemRepository;
import com.side.hhplusecommerce.item.repository.ItemViewRepository;
import com.side.hhplusecommerce.order.dto.ItemSalesCountDto;
import com.side.hhplusecommerce.order.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemPopularityService {

    private final ItemRepository itemRepository;
    private final ItemViewRepository itemViewRepository;
    private final OrderItemRepository orderItemRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PopularityScoreCalculator scoreCalculator;


    /**
     * Redis ZSET을 사용한 인기 상품 조회 (기간 선택 가능)
     * - Redis Sorted Set에서 score 기준으로 상위 상품 조회
     * - 실시간 조회수/판매량 반영
     *
     * @param period 조회 기간 (DAILY, WEEKLY)
     * @return 인기 상품 목록
     */
    public PopularItemsDto getPopularItems(PopularityPeriod period) {
        return getPopularItemsFromZSet(period.getRedisKey(), POPULAR_ITEMS_LIMIT);
    }

    /**
     * Redis ZSET에서 인기 상품 조회
     *
     * @param zsetKey Redis ZSET 키
     * @param limit 조회할 인기 상품 개수
     * @return 인기 상품 목록
     */
    private PopularItemsDto getPopularItemsFromZSet(String zsetKey, int limit) {
        try {
            // Redis ZSET에서 score 역순으로 상위 limit개 조회
            Set<Object> topItemIds = redisTemplate.opsForZSet()
                    .reverseRange(zsetKey, 0, limit - 1);

            if (topItemIds == null || topItemIds.isEmpty()) {
                log.warn("No popular items found in Redis ZSET: {}", zsetKey);
                return PopularItemsDto.of(List.of());
            }

            // String -> Long 변환
            List<Long> itemIds = topItemIds.stream()
                    .map(obj -> Long.valueOf(obj.toString()))
                    .toList();

            // Item 조회 및 DTO 변환
            List<Item> items = itemRepository.findAllByItemIdIn(itemIds);
            Map<Long, Item> itemMap = items.stream()
                    .collect(Collectors.toMap(Item::getItemId, item -> item));

            // 순서 유지하며 DTO 변환
            List<PopularItemDto> popularItems = itemIds.stream()
                    .map(itemMap::get)
                    .filter(Objects::nonNull)
                    .map(PopularItemDto::from)
                    .toList();

            return PopularItemsDto.of(popularItems);

        } catch (Exception e) {
            log.error("Failed to get popular items from Redis ZSET: {}", zsetKey, e);
            return PopularItemsDto.of(List.of());
        }
    }

    /**
     * 캐시를 우회하여 DB에서 직접 인기 상품 데이터 조회
     * 스케줄러에서 캐시 갱신 시 사용
     *
     * @param limit 조회할 인기 상품 개수
     * @return 인기 상품 목록 (캐시 미사용)
     */
    @Transactional(readOnly = true)
    public PopularItemsDto getPopularItemsFromDB(int limit) {
        log.debug("Fetching popular items from DB (bypassing cache), limit={}", limit);

        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);

        // 조회수 집계
        Map<Long, Long> viewCountMap = itemViewRepository.countViewsByItemIdGrouped(threeDaysAgo)
                .stream()
                .collect(Collectors.toMap(
                        ItemViewCountDto::getItemId,
                        ItemViewCountDto::getViewCount
                ));

        // 판매량 집계
        Map<Long, Long> salesCountMap = orderItemRepository.countSalesByItemIdGrouped(threeDaysAgo)
                .stream()
                .collect(Collectors.toMap(
                        ItemSalesCountDto::getItemId,
                        ItemSalesCountDto::getSalesCount
                ));

        // 모든 itemId 수집
        Set<Long> itemIds = new HashSet<>();
        itemIds.addAll(viewCountMap.keySet());
        itemIds.addAll(salesCountMap.keySet());

        // 인기도 계산 및 정렬
        List<Long> popularItemIds = itemIds.stream()
                .map(itemId -> {
                    long viewCount = viewCountMap.getOrDefault(itemId, 0L);
                    long salesCount = salesCountMap.getOrDefault(itemId, 0L);
                    long popularityScore = (long) scoreCalculator.calculate(viewCount, salesCount);
                    return new ItemPopularity(itemId, popularityScore, viewCount, salesCount);
                })
                .sorted(Comparator.comparing(ItemPopularity::popularityScore).reversed())
                .limit(limit)
                .map(ItemPopularity::itemId)
                .toList();

        // Item 조회 및 DTO 변환
        List<Item> items = itemRepository.findAllByItemIdIn(popularItemIds);
        Map<Long, Item> itemMap = items.stream()
                .collect(Collectors.toMap(Item::getItemId, item -> item));

        // 인기도 순서대로 정렬하여 DTO 반환
        List<PopularItemDto> popularItems = popularItemIds.stream()
                .map(itemMap::get)
                .filter(Objects::nonNull)
                .map(PopularItemDto::from)
                .toList();

        return PopularItemsDto.of(popularItems);
    }


    /**
     * Redis Sorted Set에 인기 상품 랭킹 업데이트
     *
     * @param zsetKey Redis ZSET 키
     * @param since 집계 시작 시점 (예: 1일 전, 7일 전)
     */
    public void updatePopularItemsRanking(String zsetKey, LocalDateTime since) {
        try {
            // 조회수 집계
            Map<Long, Long> viewCountMap = itemViewRepository.countViewsByItemIdGrouped(since)
                    .stream()
                    .collect(Collectors.toMap(
                            ItemViewCountDto::getItemId,
                            ItemViewCountDto::getViewCount
                    ));

            // 판매량 집계
            Map<Long, Long> salesCountMap = orderItemRepository.countSalesByItemIdGrouped(since)
                    .stream()
                    .collect(Collectors.toMap(
                            ItemSalesCountDto::getItemId,
                            ItemSalesCountDto::getSalesCount
                    ));

            // 모든 itemId 수집
            Set<Long> itemIds = new HashSet<>();
            itemIds.addAll(viewCountMap.keySet());
            itemIds.addAll(salesCountMap.keySet());

            // 상위 100개 상품 선정 (score 계산 후 정렬)
            List<ItemPopularity> topItems = itemIds.stream()
                    .map(itemId -> {
                        long viewCount = viewCountMap.getOrDefault(itemId, 0L);
                        long salesCount = salesCountMap.getOrDefault(itemId, 0L);
                        double score = scoreCalculator.calculate(viewCount, salesCount);
                        return new ItemPopularity(itemId, (long) score, viewCount, salesCount);
                    })
                    .sorted(Comparator.comparing(ItemPopularity::popularityScore).reversed())
                    .limit(POPULAR_ITEMS_LIMIT)
                    .toList();

            // 기존 Sorted Set 삭제
            redisTemplate.delete(zsetKey);

            // 새로운 랭킹 데이터 저장
            for (ItemPopularity item : topItems) {
                redisTemplate.opsForZSet().add(
                        zsetKey,
                        item.itemId().toString(),
                        item.popularityScore()
                );
            }

            log.info("Updated popular items ranking in Redis ZSET: {}. Total items: {}", zsetKey, topItems.size());

        } catch (Exception e) {
            log.error("Failed to update popular items ranking in Redis ZSET: {}", zsetKey, e);
        }
    }

    /**
     * 상품 조회 시 일간/주간 ZSET의 score 증가
     *
     * @param itemId 조회된 상품 ID
     */
    public void incrementViewScore(Long itemId) {
        int viewWeight = VIEW_SCORE_WEIGHT;

        try {
            // 일간 ZSET score 증가
            incrementScoreIfExists(POPULAR_ITEMS_DAILY_KEY, itemId, viewWeight);
            // 주간 ZSET score 증가
            incrementScoreIfExists(POPULAR_ITEMS_WEEKLY_KEY, itemId, viewWeight);

            log.debug("Incremented view score for itemId={}, weight={}", itemId, viewWeight);
        } catch (Exception e) {
            log.error("Failed to increment view score for itemId={}", itemId, e);
        }
    }

    /**
     * 상품 구매 시 일간/주간 ZSET의 score 증가
     *
     * @param itemId 구매된 상품 ID
     */
    public void incrementSalesScore(Long itemId) {
        int salesWeight = SALES_SCORE_WEIGHT;

        try {
            // 일간 ZSET score 증가
            incrementScoreIfExists(POPULAR_ITEMS_DAILY_KEY, itemId, salesWeight);
            // 주간 ZSET score 증가
            incrementScoreIfExists(POPULAR_ITEMS_WEEKLY_KEY, itemId, salesWeight);

            log.debug("Incremented sales score for itemId={}, weight={}", itemId, salesWeight);
        } catch (Exception e) {
            log.error("Failed to increment sales score for itemId={}", itemId, e);
        }
    }

    /**
     * Redis ZSET에 itemId가 존재하면 score 증가
     *
     * @param zsetKey Redis ZSET 키
     * @param itemId 상품 ID
     * @param weight 가중치
     */
    private void incrementScoreIfExists(String zsetKey, Long itemId, int weight) {
        Double currentScore = redisTemplate.opsForZSet().score(zsetKey, itemId.toString());
        if (currentScore != null) {
            redisTemplate.opsForZSet().incrementScore(zsetKey, itemId.toString(), weight);
        }
    }

    private record ItemPopularity(Long itemId, Long popularityScore, Long viewCount, Long salesCount) {}
}
