package com.side.hhplusecommerce.order.event;

import lombok.Getter;

@Getter
public class CompensateCouponCommand {
    private final Long orderId;
    private final Long userCouponId;

    private CompensateCouponCommand(Long orderId, Long userCouponId) {
        this.orderId = orderId;
        this.userCouponId = userCouponId;
    }

    public static CompensateCouponCommand of(Long orderId, Long userCouponId) {
        return new CompensateCouponCommand(orderId, userCouponId);
    }
}