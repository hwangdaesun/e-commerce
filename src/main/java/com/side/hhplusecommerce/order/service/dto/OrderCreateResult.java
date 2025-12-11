package com.side.hhplusecommerce.order.service.dto;

import com.side.hhplusecommerce.order.domain.Order;
import com.side.hhplusecommerce.order.domain.OrderItem;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * Order와 OrderItem 리스트로부터 OrderCreateResult 생성
     */
    public static OrderCreateResult from(Order order, List<OrderItem> orderItems) {
        List<OrderItemDto> orderItemDtos = orderItems.stream()
                .map(OrderItemDto::from)
                .collect(Collectors.toList());

        return OrderCreateResult.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .couponDiscount(order.getCouponDiscount())
                .finalAmount(order.getFinalAmount())
                .createdAt(order.getCreatedAt())
                .orderItems(orderItemDtos)
                .build();
    }

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

        /**
         * OrderItem으로부터 OrderItemDto 생성
         */
        public static OrderItemDto from(OrderItem orderItem) {
            return OrderItemDto.builder()
                    .orderItemId(orderItem.getOrderItemId())
                    .orderId(orderItem.getOrderId())
                    .itemId(orderItem.getItemId())
                    .name(orderItem.getName())
                    .price(orderItem.getPrice())
                    .quantity(orderItem.getQuantity())
                    .userCouponId(orderItem.getUserCouponId())
                    .build();
        }
    }
}