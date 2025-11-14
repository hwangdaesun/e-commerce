package com.side.hhplusecommerce.coupon.domain;

import com.side.hhplusecommerce.coupon.exception.AlreadyUsedCouponException;
import com.side.hhplusecommerce.coupon.exception.ExpiredCouponException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "user_coupons", indexes = {
        @Index(name = "idx_user_coupons_user_id", columnList = "user_id"),
        @Index(name = "idx_user_coupons_coupon_id", columnList = "coupon_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_coupon_id")
    private Long userCouponId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Column(name = "is_used", nullable = false)
    private Boolean isUsed;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "issued_at", nullable = false)
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
