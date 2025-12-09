package com.side.hhplusecommerce.item.constants;

/**
 * 인기도 관련 상수 관리 클래스
 */
public final class PopularityConstants {

    private PopularityConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Redis Sorted Set 키 - 일간 인기 상품
     */
    public static final String POPULAR_ITEMS_DAILY_KEY = "popular:items:daily";

    /**
     * Redis Sorted Set 키 - 주간 인기 상품
     */
    public static final String POPULAR_ITEMS_WEEKLY_KEY = "popular:items:weekly";

    /**
     * 인기 상품 랭킹에 저장할 상품 개수
     */
    public static final int POPULAR_ITEMS_LIMIT = 100;

    /**
     * 조회수 가중치 (score 계산 시 사용)
     */
    public static final int VIEW_SCORE_WEIGHT = 2;

    /**
     * 판매량 가중치 (score 계산 시 사용)
     */
    public static final int SALES_SCORE_WEIGHT = 8;
}