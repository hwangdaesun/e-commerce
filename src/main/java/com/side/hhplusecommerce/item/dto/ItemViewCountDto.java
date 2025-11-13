package com.side.hhplusecommerce.item.dto;

import lombok.Getter;

@Getter
public class ItemViewCountDto {
    private final Long itemId;
    private final Long viewCount;

    public ItemViewCountDto(Long itemId, Long viewCount) {
        this.itemId = itemId;
        this.viewCount = viewCount;
    }
}