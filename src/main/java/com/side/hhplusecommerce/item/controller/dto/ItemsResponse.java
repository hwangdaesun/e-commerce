package com.side.hhplusecommerce.item.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class ItemsResponse {
    private List<ItemInfo> items;
    private Long nextCursor;
    private Boolean hasNext;

    @Getter
    @AllArgsConstructor
    public static class ItemInfo {
        private Long itemId;
        private String name;
        private Integer price;
        private Integer stock;
        private LocalDateTime createdAt;
    }
}