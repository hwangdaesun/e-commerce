package com.side.hhplusecommerce.order.domain;

import com.side.hhplusecommerce.order.exception.InvalidOrderItemItemIdException;
import com.side.hhplusecommerce.order.exception.InvalidOrderItemOrderIdException;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.util.Objects;

@Getter
public class OrderItem {
    private Long orderItemId;
    private Long orderId;
    private Long itemId;
    private String name;
    private Integer price;
    private Integer quantity;
    private Long userCouponId;

    @Builder(access = AccessLevel.PRIVATE)
    private OrderItem(Long orderItemId, Long orderId, Long itemId, String name, Integer price, Integer quantity, Long userCouponId) {
        this.orderItemId = orderItemId;
        this.orderId = orderId;
        this.itemId = itemId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.userCouponId = userCouponId;
    }

    public static OrderItem create(Long orderId, Long itemId, String name, Integer price, Integer quantity, Long userCouponId) {
        validateReferentialIntegrity(orderId, itemId);
        return OrderItem.builder()
                .orderId(orderId)
                .itemId(itemId)
                .name(name)
                .price(price)
                .quantity(quantity)
                .userCouponId(userCouponId)
                .build();
    }

    private static void validateReferentialIntegrity(Long orderId, Long itemId) {
        if (Objects.isNull(orderId)) {
            throw new InvalidOrderItemOrderIdException();
        }
        if (Objects.isNull(itemId)) {
            throw new InvalidOrderItemItemIdException();
        }
    }


}
