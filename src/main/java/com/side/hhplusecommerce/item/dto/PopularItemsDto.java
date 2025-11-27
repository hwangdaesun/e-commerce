package com.side.hhplusecommerce.item.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PopularItemsDto {
    private List<PopularItemDto> items;

    public static PopularItemsDto of(List<PopularItemDto> items) {
        return new PopularItemsDto(items);
    }
}
