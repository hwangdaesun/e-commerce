package com.side.hhplusecommerce.order.event;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class CompensateStockCommand {
    private final Long orderId;
    private final Map<Long, Integer> itemQuantities; // itemId -> quantity

    private CompensateStockCommand(Long orderId, Map<Long, Integer> itemQuantities) {
        this.orderId = orderId;
        this.itemQuantities = itemQuantities;
    }

    public static CompensateStockCommand of(Long orderId, Map<Long, Integer> itemQuantities) {
        return new CompensateStockCommand(orderId, itemQuantities);
    }
}