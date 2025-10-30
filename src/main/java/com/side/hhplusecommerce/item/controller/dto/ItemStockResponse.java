package com.side.hhplusecommerce.item.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ItemStockResponse {
    private Long itemId;
    private String itemName;
    private Integer stock;
}