package com.side.hhplusecommerce.order.event;

import lombok.Getter;

@Getter
public class StockReservedEvent {
    private final Long orderId;

    private StockReservedEvent(Long orderId) {
        this.orderId = orderId;
    }

    public static StockReservedEvent of(Long orderId) {
        return new StockReservedEvent(orderId);
    }
}