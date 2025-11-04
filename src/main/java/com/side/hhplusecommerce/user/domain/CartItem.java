package com.side.hhplusecommerce.user.domain;

import com.side.hhplusecommerce.common.BaseEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
public class CartItem extends BaseEntity {
    private Long cartItemId;
    private Long cartId;
    private Long itemId;
    private Integer quantity;

    @Builder
    private CartItem(Long cartItemId, Long cartId, Long itemId, Integer quantity) {
        super();
        this.cartItemId = cartItemId;
        this.cartId = cartId;
        this.itemId = itemId;
        this.quantity = quantity;
    }
}