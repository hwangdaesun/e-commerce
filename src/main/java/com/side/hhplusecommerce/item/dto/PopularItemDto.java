package com.side.hhplusecommerce.item.dto;

import com.side.hhplusecommerce.item.domain.Item;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PopularItemDto {
    private Long itemId;
    private String name;
    private Integer price;
    private Integer stock;

    public static PopularItemDto from(Item item) {
        return new PopularItemDto(
                item.getItemId(),
                item.getName(),
                item.getPrice(),
                item.getStock()
        );
    }
}
