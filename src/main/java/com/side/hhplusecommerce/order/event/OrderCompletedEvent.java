package com.side.hhplusecommerce.order.event;

import lombok.Getter;

@Getter
public class OrderCompletedEvent {
    private final Long orderId;

    private OrderCompletedEvent(Long orderId) {
        this.orderId = orderId;
    }

    public static OrderCompletedEvent of(Long orderId) {
        return new OrderCompletedEvent(orderId);
    }
}