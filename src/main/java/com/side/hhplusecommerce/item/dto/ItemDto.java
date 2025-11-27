package com.side.hhplusecommerce.item.dto;

import com.side.hhplusecommerce.item.domain.Item;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ItemDto {
    private Long itemId;
    private String name;
    private Integer price;
    private Integer stock;
    private LocalDateTime createdAt;

    public static ItemDto from(Item item) {
        return new ItemDto(
                item.getItemId(),
                item.getName(),
                item.getPrice(),
                item.getStock(),
                item.getCreatedAt()
        );
    }
}