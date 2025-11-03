package com.side.hhplusecommerce.item.domain;

import com.side.hhplusecommerce.item.exception.InsufficientStockException;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ItemStock {
    private Long itemId;
    private Integer stock;
    private LocalDateTime updatedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private ItemStock(Long itemId, Integer stock) {
        this.itemId = itemId;
        this.stock = stock;
        this.updatedAt = LocalDateTime.now();
    }

    public static ItemStock of(Long itemId, Integer stock) {
        return ItemStock.builder()
                .itemId(itemId)
                .stock(stock)
                .build();
    }

    public void decrease(Integer quantity) {
        if (!hasEnoughQuantity(quantity)) {
            throw new InsufficientStockException();
        }
        this.stock -= quantity;
        this.updatedAt = LocalDateTime.now();
    }
    public boolean hasEnoughQuantity(Integer quantity) {
        return this.stock >= quantity;
    }

}
