package com.side.hhplusecommerce.user.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CartItemResponse {
    private Long cartItemId;
    private Long itemId;
    private String itemName;
    private Integer price;
    private Integer quantity;
    private Integer totalPrice;
    private Integer stock;
    private LocalDateTime createdAt;
}