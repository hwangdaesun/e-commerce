package com.side.hhplusecommerce.coupon.domain;

import com.side.hhplusecommerce.coupon.exception.CouponSoldOutException;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CouponStock {
    private Long couponId;
    private Integer remainingQuantity;
    private LocalDateTime updatedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private CouponStock(Long couponId, Integer remainingQuantity) {
        this.couponId = couponId;
        this.remainingQuantity = remainingQuantity;
        this.updatedAt = LocalDateTime.now();
    }

    public static CouponStock of(Long couponId, Integer totalQuantity) {
        return CouponStock.builder()
                .couponId(couponId)
                .remainingQuantity(totalQuantity)
                .build();
    }

    public void decrease() {
        if (!hasRemainingQuantity()) {
            throw new CouponSoldOutException();
        }
        this.remainingQuantity--;
        this.updatedAt = LocalDateTime.now();
    }

    public void increase() {
        this.remainingQuantity++;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean hasRemainingQuantity() {
        return this.remainingQuantity > 0;
    }
}
