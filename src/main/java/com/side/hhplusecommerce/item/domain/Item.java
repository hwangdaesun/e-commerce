package com.side.hhplusecommerce.item.domain;

import com.side.hhplusecommerce.common.BaseEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
public class Item extends BaseEntity {
    private Long itemId;
    private String name;
    private Integer price;

    @Builder
    private Item(Long itemId, String name, Integer price) {
        super();
        this.itemId = itemId;
        this.name = name;
        this.price = price;
    }
}