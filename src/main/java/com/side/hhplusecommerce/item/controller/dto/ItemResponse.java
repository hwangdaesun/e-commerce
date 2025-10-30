package com.side.hhplusecommerce.item.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ItemResponse {
    private Long itemId;
    private String name;
    private Integer price;
    private Integer stock;
    private LocalDateTime createdAt;
}
