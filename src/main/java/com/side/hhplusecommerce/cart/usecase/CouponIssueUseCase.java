package com.side.hhplusecommerce.cart.usecase;

import com.side.hhplusecommerce.coupon.controller.dto.IssueCouponResponse;
import com.side.hhplusecommerce.coupon.service.CouponIssueLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponIssueUseCase {
    private final CouponIssueLockService couponIssueLockService;

    public IssueCouponResponse issue(Long couponId, Long userId) {
        return couponIssueLockService.issueCouponWithDistributedLock(couponId, userId);
    }
}
