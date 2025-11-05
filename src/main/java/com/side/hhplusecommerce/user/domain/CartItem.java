package com.side.hhplusecommerce.user.domain;

import com.side.hhplusecommerce.common.BaseEntity;
import com.side.hhplusecommerce.user.exception.InvalidCartItemQuantityException;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class CartItem extends BaseEntity {
    private Long cartItemId;
    private Long cartId;
    private Long itemId;
    private Integer quantity;

    @Builder(access = AccessLevel.PRIVATE)
    private CartItem(Long cartItemId, Long cartId, Long itemId, Integer quantity) {
        super();
        this.cartItemId = cartItemId;
        this.cartId = cartId;
        this.itemId = itemId;
        this.quantity = quantity;
    }

    public static CartItem create(Long cartId, Long itemId, Integer quantity) {
        validateQuantity(quantity);
        return CartItem.builder()
                .cartItemId(cartItemId)
                .cartId(cartId)
                .itemId(itemId)
                .quantity(quantity)
                .build();
    }

    private static void validateQuantity(Integer quantity) {
        if (quantity < 1) {
            throw new InvalidCartItemQuantityException();
        }
    }

    public void updateQuantity(Integer quantity) {
        validateQuantity(quantity);
        this.quantity = quantity;
    }

    public Integer calculateTotalPrice(Integer itemPrice) {
        return itemPrice * this.quantity;
    }
}
