package com.side.hhplusecommerce.item.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.side.hhplusecommerce.item.constants.PopularityConstants.POPULAR_ITEMS_DAILY_KEY;
import static com.side.hhplusecommerce.item.constants.PopularityConstants.POPULAR_ITEMS_WEEKLY_KEY;

/**
 * 인기 상품 조회 기간 타입
 */
@Getter
@RequiredArgsConstructor
public enum PopularityPeriod {
    DAILY(POPULAR_ITEMS_DAILY_KEY, "일간"),
    WEEKLY(POPULAR_ITEMS_WEEKLY_KEY, "주간");

    private final String redisKey;
    private final String description;
}
