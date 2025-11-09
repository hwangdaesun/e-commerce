package com.side.hhplusecommerce.coupon.domain;

import com.side.hhplusecommerce.common.BaseEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Coupon extends BaseEntity {
    private Long couponId;
    private String name;
    private Integer discountAmount;
    private Integer totalQuantity;
    private LocalDateTime expiresAt;

    @Builder
    private Coupon(Long couponId, String name, Integer discountAmount, Integer totalQuantity, LocalDateTime expiresAt) {
        super();
        this.couponId = couponId;
        this.name = name;
        this.discountAmount = discountAmount;
        this.totalQuantity = totalQuantity;
        this.expiresAt = expiresAt;
    }
}