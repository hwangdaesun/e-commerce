package com.side.hhplusecommerce.order.event;

import com.side.hhplusecommerce.order.domain.Order;
import lombok.Getter;

@Getter
public class ProcessPaymentEvent {
    private final Long orderId;
    private final Long userId;
    private final Integer finalAmount;

    private ProcessPaymentEvent(Long orderId, Long userId, Integer finalAmount) {
        this.orderId = orderId;
        this.userId = userId;
        this.finalAmount = finalAmount;
    }

    public static ProcessPaymentEvent from(Order order) {
        return new ProcessPaymentEvent(order.getOrderId(), order.getUserId(), order.getFinalAmount());
    }
}