package com.side.hhplusecommerce.item.domain;

import com.side.hhplusecommerce.common.BaseEntity;
import com.side.hhplusecommerce.item.exception.InsufficientStockException;
import com.side.hhplusecommerce.item.exception.InvalidSalesQuantityException;
import java.util.Objects;
import lombok.Builder;
import lombok.Getter;

@Getter
public class Item extends BaseEntity {
    private Long itemId;
    private String name;
    private Integer price;
    private Integer stock;
    private Integer salesCount;

    @Builder
    private Item(Long itemId, String name, Integer price, Integer stock, Integer salesCount) {
        super();
        this.itemId = itemId;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.salesCount = salesCount != null ? salesCount : 0;
    }

    public void decrease(Integer quantity) {
        if (!hasEnoughQuantity(quantity)) {
            throw new InsufficientStockException();
        }
        this.stock -= quantity;
    }

    public void increase(Integer quantity) {
        this.stock += quantity;
    }

    public boolean hasEnoughQuantity(Integer quantity) {
        return this.stock >= quantity;
    }

    public void increaseSalesCount(Integer quantity) {
        if (Objects.isNull(quantity) || quantity <= 0) {
            throw new InvalidSalesQuantityException();
        }
        this.salesCount += quantity;
    }
}