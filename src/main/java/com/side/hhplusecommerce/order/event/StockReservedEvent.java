package com.side.hhplusecommerce.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservedEvent {
    private Long orderId;

    public static StockReservedEvent of(Long orderId) {
        return new StockReservedEvent(orderId);
    }
}