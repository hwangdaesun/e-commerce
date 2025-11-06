package com.side.hhplusecommerce.coupon.domain;

import com.side.hhplusecommerce.coupon.exception.AlreadyUsedCouponException;
import com.side.hhplusecommerce.coupon.exception.ExpiredCouponException;
import lombok.AccessLevel;
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

    public static UserCoupon createWithId(Long userCouponId, Long userId, Long couponId, Boolean isUsed, LocalDateTime usedAt, LocalDateTime issuedAt) {
        return UserCoupon.builder()
                .userCouponId(userCouponId)
                .userId(userId)
                .couponId(couponId)
                .isUsed(isUsed)
                .usedAt(usedAt)
                .issuedAt(issuedAt)
                .build();
    }

    public void use(LocalDateTime expiresAt) {
        if (Boolean.TRUE.equals(this.isUsed)) {
            throw new AlreadyUsedCouponException();
        }
        if (isExpired(expiresAt)) {
            throw new ExpiredCouponException();
        }
        this.isUsed = true;
        this.usedAt = LocalDateTime.now();
    }

    public void cancelUse() {
        this.isUsed = false;
        this.usedAt = null;
    }

    public boolean isExpired(LocalDateTime expiresAt) {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
