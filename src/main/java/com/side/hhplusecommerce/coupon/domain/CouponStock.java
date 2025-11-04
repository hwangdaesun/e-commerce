package com.side.hhplusecommerce.coupon.domain;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

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
}
