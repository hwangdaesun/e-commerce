package com.side.hhplusecommerce.order.event;

import com.side.hhplusecommerce.cart.domain.CartItem;
import com.side.hhplusecommerce.item.domain.Item;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    private Long orderId;
    private Long userId;
    private List<Long> cartItemIds;
    private List<Long> itemIds;
    private Long userCouponId;

    /**
     * OrderCreatedEvent 생성 팩토리 메서드
     */
    public static OrderCreatedEvent of(Long orderId, Long userId, List<CartItem> cartItems, List<Item> items, Long userCouponId) {
        List<Long> cartItemIds = cartItems.stream()
                .map(CartItem::getCartItemId)
                .toList();

        List<Long> itemIds = items.stream()
                .map(Item::getItemId)
                .toList();

        return new OrderCreatedEvent(orderId, userId, cartItemIds, itemIds, userCouponId);
    }
}
