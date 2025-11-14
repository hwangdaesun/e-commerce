package com.side.hhplusecommerce.order.dto;

import lombok.Getter;

@Getter
public class ItemSalesCountDto {
    private final Long itemId;
    private final Long salesCount;

    public ItemSalesCountDto(Long itemId, Long salesCount) {
        this.itemId = itemId;
        this.salesCount = salesCount;
    }
}