package com.side.hhplusecommerce.coupon.service;

import com.side.hhplusecommerce.common.lock.distributed.DistributedLock;
import com.side.hhplusecommerce.coupon.controller.dto.IssueCouponResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 쿠폰 발급 서비스 - DistributedLock 레이어
 * 분산락만 적용하고 트랜잭션은 내부 서비스에서 처리합니다.
 * 실행 순서: 락 획득 → 트랜잭션 시작 → 로직 실행 → 트랜잭션 커밋 → 락 해제
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CouponIssueLockService {
    private final CouponIssueTransactionService couponIssueTransactionService;

    /**
     * 쿠폰 발급 (DistributedLock 적용)
     * 동일한 couponId에 대해 동시에 실행되지 않도록 분산락을 사용합니다.
     */
    @DistributedLock(keyResolver = "couponIssueLockKeyResolver", key = "#couponId")
    public IssueCouponResponse issueCouponWithDistributedLock(Long couponId, Long userId) {
        return couponIssueTransactionService.issueCoupon(couponId, userId);
    }
}
