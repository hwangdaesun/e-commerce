package com.side.hhplusecommerce.order.event;

import com.side.hhplusecommerce.cart.domain.CartItem;
import com.side.hhplusecommerce.item.domain.Item;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class OrderCreatedEvent {
    private final Long orderId;
    private final Long userId;
    private final List<Long> cartItemIds;
    private final List<Long> itemIds;
    private final Long userCouponId;

    private OrderCreatedEvent(Long orderId, Long userId, List<Long> cartItemIds, List<Long> itemIds, Long userCouponId) {
        this.orderId = orderId;
        this.userId = userId;
        this.cartItemIds = cartItemIds;
        this.itemIds = itemIds;
        this.userCouponId = userCouponId;
    }

    /**
     * OrderCreatedEvent 생성 팩토리 메서드
     */
    public static OrderCreatedEvent of(Long orderId, Long userId, List<CartItem> cartItems, List<Item> items, Long userCouponId) {
        List<Long> cartItemIds = cartItems.stream()
                .map(CartItem::getCartItemId)
                .collect(Collectors.toList());

        List<Long> itemIds = items.stream()
                .map(Item::getItemId)
                .collect(Collectors.toList());

        return new OrderCreatedEvent(orderId, userId, cartItemIds, itemIds, userCouponId);
    }
}