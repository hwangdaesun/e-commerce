package com.side.hhplusecommerce.cart.domain;

import com.side.hhplusecommerce.common.BaseEntity;
import com.side.hhplusecommerce.cart.exception.InvalidCartItemQuantityException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "cart_items", indexes = {
        @Index(name = "idx_cart_items_cart_id", columnList = "cart_id"),
        @Index(name = "idx_cart_items_item_id", columnList = "item_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_item_id")
    private Long cartItemId;

    @Column(name = "cart_id", nullable = false)
    private Long cartId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "quantity", nullable = false)
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
                .cartId(cartId)
                .itemId(itemId)
                .quantity(quantity)
                .build();
    }

    public static CartItem createWithId(Long cartItemId, Long cartId, Long itemId, Integer quantity) {
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
