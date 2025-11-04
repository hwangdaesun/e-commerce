package com.side.hhplusecommerce.coupon.domain;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class UserCoupon {
    private Long userCouponId;
    private Long userId;
    private Long couponId;
    private Boolean isUsed;
    private LocalDateTime usedAt;
    private LocalDateTime issuedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private UserCoupon(Long userCouponId, Long userId, Long couponId, Boolean isUsed, LocalDateTime usedAt, LocalDateTime issuedAt) {
        this.userCouponId = userCouponId;
        this.userId = userId;
        this.couponId = couponId;
        this.isUsed = isUsed;
        this.usedAt = usedAt;
        this.issuedAt = issuedAt;
    }

    public static UserCoupon issue(Long userId, Long couponId) {
        return UserCoupon.builder()
                .userId(userId)
                .couponId(couponId)
                .isUsed(false)
                .issuedAt(LocalDateTime.now())
                .build();
    }

}
