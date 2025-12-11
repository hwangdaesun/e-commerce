package com.side.hhplusecommerce.order.event;

import lombok.Getter;

@Getter
public class CouponUsedEvent {
    private final Long orderId;

    private CouponUsedEvent(Long orderId) {
        this.orderId = orderId;
    }

    public static CouponUsedEvent of(Long orderId) {
        return new CouponUsedEvent(orderId);
    }
}