package com.side.hhplusecommerce.order.event;

import com.side.hhplusecommerce.order.domain.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPaymentEvent {
    private Long orderId;
    private Long userId;
    private Integer finalAmount;

    public static ProcessPaymentEvent from(Order order) {
        return new ProcessPaymentEvent(order.getOrderId(), order.getUserId(), order.getFinalAmount());
    }
}