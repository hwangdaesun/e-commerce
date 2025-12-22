package com.side.hhplusecommerce.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompensateStockCommand {
    private Long orderId;
    private Map<Long, Integer> itemQuantities; // itemId -> quantity

    public static CompensateStockCommand of(Long orderId, Map<Long, Integer> itemQuantities) {
        return new CompensateStockCommand(orderId, itemQuantities);
    }
}