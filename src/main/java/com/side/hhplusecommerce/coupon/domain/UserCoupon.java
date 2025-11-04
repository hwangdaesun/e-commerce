package com.side.hhplusecommerce.coupon.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserCoupon {
    private Long userCouponId;
    private Long userId;
    private Long couponId;
    private Boolean isUsed;
    private LocalDateTime usedAt;
    private LocalDateTime issuedAt;

    @Builder
    private UserCoupon(Long userCouponId, Long userId, Long couponId, Boolean isUsed, LocalDateTime usedAt, LocalDateTime issuedAt) {
        this.userCouponId = userCouponId;
        this.userId = userId;
        this.couponId = couponId;
        this.isUsed = isUsed;
        this.usedAt = usedAt;
        this.issuedAt = issuedAt;
    }
}