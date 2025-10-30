package com.side.hhplusecommerce.user.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CartItemRequest {
    private Long userId;
    private Long itemId;
    private Integer quantity;
}