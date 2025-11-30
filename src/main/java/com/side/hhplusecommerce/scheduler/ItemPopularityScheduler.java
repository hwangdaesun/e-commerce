package com.side.hhplusecommerce.scheduler;

import com.side.hhplusecommerce.item.dto.ItemViewCountDto;
import com.side.hhplusecommerce.item.dto.PopularItemsDto;
import com.side.hhplusecommerce.item.repository.ItemViewRepository;
import com.side.hhplusecommerce.item.service.ItemPopularityService;
import com.side.hhplusecommerce.order.dto.ItemSalesCountDto;
import com.side.hhplusecommerce.order.repository.OrderItemRepository;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ItemPopularityScheduler {

    private final ItemViewRepository itemViewRepository;
    private final OrderItemRepository orderItemRepository;
    private final ItemPopularityService itemPopularityService;
    private final CacheManager cacheManager;

    // 캐싱할 limit 값들 (고정된 값으로 관리)
    private static final List<Integer> CACHE_LIMITS = List.of(5, 10, 20);

    /**
     * 매 시간마다 상품 인기도 집계 테이블 갱신
     * - cron: 매 시간 0분에 실행
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void updatePopularityStats() {
        log.info("Starting popularity stats update...");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeDaysAgo = now.minusDays(3);

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

        // bulk insert로 통계 데이터 저장
        itemPopularityService.bulkInsertPopularityStats(viewCountMap, salesCountMap, itemIds, now);

        log.info("Popularity stats update completed.");
    }

    /**
     * 매 정각마다 인기 상품 캐시 갱신 (임시 키 + RENAME 전략)
     * - cron: 매 시간 0분에 실행
     *
     * 캐시 스탬피드 방지 전략: 임시 키 저장 후 RENAME
     * 1. DB에서 인기 상품 데이터 조회
     * 2. 임시 키(popular-items::limit:temp)에 저장 (TTL 25시간)
     * 3. Redis RENAME 명령으로 임시 키를 서비스 키로 원자적 교체
     * 4. 데이터 공백 0초 보장 (기존 키가 있으면 즉시 교체, 없으면 그대로 사용)
     */
    @Scheduled(cron = "0 0 * * * *")
    public void refreshPopularItemsCache() {
        log.info("Starting popular items cache refresh with RENAME strategy...");

        for (Integer limit : CACHE_LIMITS) {
            try {
                // 1. DB에서 최신 인기 상품 데이터 조회 (캐시 우회)
                PopularItemsDto freshData = itemPopularityService.getPopularItemsFromDB(limit);

                // 2. 임시 키에 저장 후 RENAME으로 원자적 교체
                itemPopularityService.refreshCacheWithRename(limit, freshData);
                // DB 부하 분산을 위한 짧은 지연
                Thread.sleep(100);

            } catch (Exception e) {
                log.error("Failed to refresh cache for limit={}", limit, e);
            }
        }
    }
    
}
