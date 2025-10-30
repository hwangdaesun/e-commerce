package com.side.hhplusecommerce.user.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CartResponse {
    private List<CartItem> items;
    private Summary summary;

    @Getter
    @AllArgsConstructor
    public static class CartItem {
        private Long cartItemId;
        private Long itemId;
        private String itemName;
        private Integer price;
        private Integer quantity;
        private Integer totalPrice;
        private Integer stock;
    }

    @Getter
    @AllArgsConstructor
    public static class Summary {
        private Integer totalItems;
        private Integer totalQuantity;
        private Integer totalAmount;
    }
}