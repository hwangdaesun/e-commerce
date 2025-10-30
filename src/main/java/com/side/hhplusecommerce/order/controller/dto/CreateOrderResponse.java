package com.side.hhplusecommerce.order.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class CreateOrderResponse {
    private Long orderId;
    private List<OrderItem> orderItems;
    private Integer totalAmount;
    private Integer couponDiscount;
    private Integer finalAmount;
    private Integer paymentAmount;
    private CouponUsed couponUsed;
    private LocalDateTime orderedAt;

    @Getter
    @AllArgsConstructor
    public static class OrderItem {
        private Long orderItemId;
        private Long itemId;
        private String itemName;
        private Integer price;
        private Integer quantity;
        private Integer totalPrice;
    }

    @Getter
    @AllArgsConstructor
    public static class CouponUsed {
        private Long userCouponId;
        private String couponName;
        private Integer discountAmount;
    }
}