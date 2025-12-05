package com.side.hhplusecommerce.cart.usecase;

import com.side.hhplusecommerce.coupon.controller.dto.IssueCouponResponse;
import com.side.hhplusecommerce.coupon.infrastructure.redis.CouponIssueQueueProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 쿠폰 발급 UseCase
 * Redis Stream을 이용한 비동기 큐 방식으로 쿠폰 발급을 처리합니다.
 *
 * 흐름:
 * 1. Producer에서 큐에 발급 요청 추가 (Redis 재고 확인, 중복 발급 확인)
 * 2. 응답 즉시 반환 (발급 요청 접수됨)
 * 3. Consumer에서 비동기로 실제 발급 처리 (MySQL 저장)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CouponIssueUseCase {
    private final CouponIssueQueueProducer couponIssueQueueProducer;

    /**
     * 쿠폰 발급 요청
     * 큐에 추가만 하고 즉시 응답을 반환합니다.
     *
     * @param couponId 쿠폰 ID
     * @param userId 사용자 ID
     * @return 발급 요청 접수 응답 (null - 비동기 처리)
     */
    public IssueCouponResponse issue(Long couponId, Long userId) {
        // Redis Stream 큐에 발급 요청 추가
        couponIssueQueueProducer.enqueue(couponId, userId);

        log.info("쿠폰 발급 요청 접수: couponId={}, userId={}", couponId, userId);

        // 비동기 처리이므로 null 반환
        // 실제 발급은 Consumer에서 처리되며, 사용자는 발급된 쿠폰을 조회 API로 확인
        return null;
    }
}
