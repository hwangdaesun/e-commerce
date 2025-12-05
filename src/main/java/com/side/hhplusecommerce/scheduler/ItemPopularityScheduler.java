package com.side.hhplusecommerce.scheduler;

import static com.side.hhplusecommerce.item.constants.PopularityConstants.POPULAR_ITEMS_DAILY_KEY;
import static com.side.hhplusecommerce.item.constants.PopularityConstants.POPULAR_ITEMS_WEEKLY_KEY;

import com.side.hhplusecommerce.item.service.ItemPopularityService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ItemPopularityScheduler {

    private final ItemPopularityService itemPopularityService;

    /**
     * 매 시간마다 일간 인기 상품 랭킹 갱신 (최근 1일 기준)
     * - cron: 매 시간 0분에 실행
     */
    @Scheduled(cron = "0 0 * * * *")
    public void refreshDailyPopularItems() {
        log.info("Starting daily popular items ranking update...");
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        itemPopularityService.updatePopularItemsRanking(POPULAR_ITEMS_DAILY_KEY, oneDayAgo);
        log.info("Daily popular items ranking update completed.");
    }

    /**
     * 매 시간마다 주간 인기 상품 랭킹 갱신 (최근 7일 기준)
     * - cron: 매 시간 0분에 실행
     */
    @Scheduled(cron = "0 0 * * * *")
    public void refreshWeeklyPopularItems() {
        log.info("Starting weekly popular items ranking update...");
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        itemPopularityService.updatePopularItemsRanking(POPULAR_ITEMS_WEEKLY_KEY, sevenDaysAgo);
        log.info("Weekly popular items ranking update completed.");
    }
}
