package com.side.hhplusecommerce.item.service;

import static com.side.hhplusecommerce.config.RedisCacheConfig.ITEM;
import static com.side.hhplusecommerce.config.RedisCacheConfig.LOW_STOCK_THRESHOLD;
import static com.side.hhplusecommerce.config.RedisCacheConfig.POPULAR_ITEMS;
import static com.side.hhplusecommerce.config.RedisCacheConfig.TEMP_SUFFIX;
import static java.time.Duration.*;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
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
import org.springframework.cache.annotation.Cacheable;
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
    private static final int VIEW_COUNT_WEIGHT = 1;
    private static final int SALES_COUNT_WEIGHT = 10;
    private static final int BATCH_SIZE = 1000;

    private final ItemRepository itemRepository;
    private final ItemViewRepository itemViewRepository;
    private final OrderItemRepository orderItemRepository;
    private final JdbcTemplate jdbcTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 케이스 1, 2: @Cacheable을 사용한 인기 상품 조회
     * - Key: "popular-items::{limit}"
     * - Value: PopularItemsDto (Wrapper 객체)
     * - TTL 1일
     * - 최근 3일 기준으로 집계
     *
     * @param limit 조회할 인기 상품 개수
     * @return 인기 상품 목록 Wrapper
     */
    @Cacheable(value = POPULAR_ITEMS, key = "#limit")
    @Transactional(readOnly = true)
    public PopularItemsDto getPopularItemsV1(int limit) {
        // 최근 3일 기준
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
                    long popularityScore = viewCount * VIEW_COUNT_WEIGHT + salesCount * SALES_COUNT_WEIGHT;
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
     * 인기 상품 상세 조회 (조건부 캐싱)
     * - 재고가 임계값 초과: 캐싱 O (성능 우선)
     * - 재고가 임계값 이하: 캐싱 X (정확도 우선)
     *
     * @param itemId 상품 ID
     * @return 상품 정보 DTO
     */
    @Cacheable(
            value = ITEM,
            key = "#itemId",
            unless = "#result != null && #result.stock <= T(com.side.hhplusecommerce.config.RedisCacheConfig).LOW_STOCK_THRESHOLD"
    )
    @Transactional(readOnly = true)
    public ItemDto getItemV1(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));
        ItemDto itemDto = ItemDto.from(item);

        if (itemDto.getStock() <= LOW_STOCK_THRESHOLD) {
            log.debug("Low-stock item will not be cached: itemId={}, stock={}",
                    itemId, itemDto.getStock());
        }

        return itemDto;
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
                    long popularityScore = viewCount * VIEW_COUNT_WEIGHT + salesCount * SALES_COUNT_WEIGHT;
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
     * 임시 키를 사용한 RENAME 전략으로 캐시 갱신
     * 캐시 스탬피드 방지를 위해 원자적 키 교체 수행
     *
     * 프로세스:
     * 1. 임시 키에 새 데이터 저장 (TTL 25시간)
     * 2. Redis RENAME 명령으로 임시 키를 서비스 키로 원자적 교체
     * 3. 데이터 공백 0초 보장
     *
     * @param limit 조회할 인기 상품 개수
     * @param freshData DB에서 조회한 최신 데이터
     */
    public void refreshCacheWithRename(int limit, PopularItemsDto freshData) {
        String serviceKey = buildCacheKey(limit);
        String tempKey = buildTempCacheKey(limit);

        try {
            // 1. 임시 키에 최신 데이터 저장 (TTL 25시간)
            redisTemplate.opsForValue().set(tempKey, freshData);
            redisTemplate.expire(tempKey, ofHours(25));

            // 2. Redis RENAME 명령으로 임자적 키 교체
            // RENAME은 원자적 연산으로, 기존 키가 있으면 덮어쓰고 없으면 새로 생성
            redisTemplate.rename(tempKey, serviceKey);

            // 3. 서비스 키에 TTL 재설정 (25시간)
            redisTemplate.expire(serviceKey, ofHours(25));

        } catch (Exception e) {
            log.error("Failed to refresh cache with RENAME for limit={}", limit, e);

            // 실패 시 임시 키 정리
            try {
                redisTemplate.delete(tempKey);
            } catch (Exception cleanupEx) {
                log.warn("Failed to clean up temp key: {}", tempKey, cleanupEx);
            }

        }
    }

    /**
     * 캐시 키 생성
     * Spring Cache의 키 형식: "cacheName::key"
     */
    private String buildCacheKey(int limit) {
        return POPULAR_ITEMS + "::" + limit;
    }

    /**
     * 임시 캐시 키 생성
     */
    private String buildTempCacheKey(int limit) {
        return POPULAR_ITEMS + "::" + limit + TEMP_SUFFIX;
    }

    private record ItemPopularity(Long itemId, Long popularityScore, Long viewCount, Long salesCount) {}
}
