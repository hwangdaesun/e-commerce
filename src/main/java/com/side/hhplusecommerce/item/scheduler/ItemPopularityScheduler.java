package com.side.hhplusecommerce.item.scheduler;

import com.side.hhplusecommerce.item.domain.ItemPopularityStats;
import com.side.hhplusecommerce.item.dto.ItemViewCountDto;
import com.side.hhplusecommerce.item.repository.ItemPopularityStatsRepository;
import com.side.hhplusecommerce.item.repository.ItemViewRepository;
import com.side.hhplusecommerce.order.dto.ItemSalesCountDto;
import com.side.hhplusecommerce.order.repository.OrderItemRepository;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ItemPopularityScheduler {

    private final ItemPopularityStatsRepository statsRepository;
    private final ItemViewRepository itemViewRepository;
    private final OrderItemRepository orderItemRepository;

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

        // 각 상품별 통계 생성
        for (Long itemId : itemIds) {
            long viewCount = viewCountMap.getOrDefault(itemId, 0L);
            long salesCount = salesCountMap.getOrDefault(itemId, 0L);

            ItemPopularityStats stats = ItemPopularityStats.create(itemId, viewCount, salesCount, now);
            statsRepository.save(stats);
        }
        // todo : 비동기 청크 단위의 BULK INSERT 수행
        // todo : 대기, 성공, 실패를 기록해야하는데 일단 로그 남기는 것으로 대체.

        log.info("Popularity stats update completed. Updated {} items", itemIds.size());
    }
}
