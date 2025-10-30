package com.side.hhplusecommerce.user.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCartItemRequest {
    private Long userId;
    private Integer quantity;
}