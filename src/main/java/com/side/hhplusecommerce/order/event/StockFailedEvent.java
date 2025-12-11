package com.side.hhplusecommerce.order.event;

import lombok.Getter;

@Getter
public class StockFailedEvent {
    private final Long orderId;
    private final String reason;

    private StockFailedEvent(Long orderId, String reason) {
        this.orderId = orderId;
        this.reason = reason;
    }

    public static StockFailedEvent of(Long orderId, String reason) {
        return new StockFailedEvent(orderId, reason);
    }
}