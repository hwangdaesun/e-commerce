package com.side.hhplusecommerce.item.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ItemStock {
    private Long itemId;
    private Integer stock;
    private LocalDateTime updatedAt;

    @Builder
    private ItemStock(Long itemId, Integer stock) {
        this.itemId = itemId;
        this.stock = stock;
        this.updatedAt = LocalDateTime.now();
    }

    public void decrease(Integer quantity) {
        if (this.stock >= quantity) {
            throw new IllegalArgumentException("재고가 부족합니다.");
        }
        this.stock -= quantity;
        this.updatedAt = LocalDateTime.now();
    }
}
