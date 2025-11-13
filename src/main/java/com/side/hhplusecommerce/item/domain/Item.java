package com.side.hhplusecommerce.item.domain;

import com.side.hhplusecommerce.common.BaseEntity;
import com.side.hhplusecommerce.item.exception.InsufficientStockException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "stock", nullable = false)
    private Integer stock;

    @Version
    @Column(name = "version")
    private Long version;

    @Builder
    private Item(Long itemId, String name, Integer price, Integer stock, Long version) {
        super();
        this.itemId = itemId;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.version = version;
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
}
