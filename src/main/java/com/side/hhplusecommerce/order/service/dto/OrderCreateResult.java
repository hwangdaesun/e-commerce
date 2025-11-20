package com.side.hhplusecommerce.order.service.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderCreateResult {
    private final Long orderId;
    private final Long userId;
    private final Integer totalAmount;
    private final Integer couponDiscount;
    private final Integer finalAmount;
    private final LocalDateTime createdAt;
    private final List<OrderItemDto> orderItems;

    @Getter
    @Builder
    public static class OrderItemDto {
        private final Long orderItemId;
        private final Long orderId;
        private final Long itemId;
        private final String name;
        private final Integer price;
        private final Integer quantity;
        private final Long userCouponId;
    }
}