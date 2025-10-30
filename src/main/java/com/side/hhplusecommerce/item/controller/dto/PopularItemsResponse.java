package com.side.hhplusecommerce.item.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PopularItemsResponse {
    private List<PopularItem> popularItems;

    @Getter
    @AllArgsConstructor
    public static class PopularItem {
        private Integer rank;
        private Long itemId;
        private String itemName;
        private Integer price;
        private Integer stock;
    }
}