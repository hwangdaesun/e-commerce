package com.side.hhplusecommerce.item.service;

import static com.side.hhplusecommerce.item.constants.PopularityConstants.SALES_SCORE_WEIGHT;
import static com.side.hhplusecommerce.item.constants.PopularityConstants.VIEW_SCORE_WEIGHT;

import org.springframework.stereotype.Component;

/**
 * 인기도 점수 계산 컴포넌트
 * 조회수와 판매량을 기반으로 상품의 인기도 점수를 계산합니다.
 */
@Component
public class PopularityScoreCalculator {

    /**
     * 인기도 점수 계산
     *
     * @param viewCount 조회수
     * @param salesCount 판매량
     * @return 인기도 점수 (조회수 * 2 + 판매량 * 8)
     */
    public double calculate(long viewCount, long salesCount) {
        return viewCount * VIEW_SCORE_WEIGHT + salesCount * SALES_SCORE_WEIGHT;
    }

    /**
     * 조회수에 대한 점수 계산
     *
     * @param viewCount 조회수
     * @return 조회수 점수
     */
    public int calculateViewScore(long viewCount) {
        return (int) (viewCount * VIEW_SCORE_WEIGHT);
    }

    /**
     * 판매량에 대한 점수 계산
     *
     * @param salesCount 판매량
     * @return 판매량 점수
     */
    public int calculateSalesScore(long salesCount) {
        return (int) (salesCount * SALES_SCORE_WEIGHT);
    }
}