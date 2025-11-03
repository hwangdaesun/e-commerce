package com.side.hhplusecommerce.order.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

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

}
