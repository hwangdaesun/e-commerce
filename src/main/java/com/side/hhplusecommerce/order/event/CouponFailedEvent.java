package com.side.hhplusecommerce.order.event;

import lombok.Getter;

@Getter
public class CouponFailedEvent {
    private final Long orderId;
    private final String reason;

    private CouponFailedEvent(Long orderId, String reason) {
        this.orderId = orderId;
        this.reason = reason;
    }

    public static CouponFailedEvent of(Long orderId, String reason) {
        return new CouponFailedEvent(orderId, reason);
    }
}