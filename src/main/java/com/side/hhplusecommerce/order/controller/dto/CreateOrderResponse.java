package com.side.hhplusecommerce.order.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Getter
@AllArgsConstructor
@Schema(description = "주문 생성 응답")
public class CreateOrderResponse {
    @Schema(description = "주문 ID", example = "1")
    private Long orderId;

    @Schema(description = "주문 항목 목록")
    private List<OrderItem> orderItems;

    @Schema(description = "총 상품 금액", example = "117000")
    private Integer totalAmount;

    @Schema(description = "쿠폰 할인 금액", example = "5000")
    private Integer couponDiscount;

    @Schema(description = "최종 결제 금액 (쿠폰 적용 후)", example = "112000")
    private Integer finalAmount;

    @Schema(description = "실제 결제된 금액 (포인트 차감 후)", example = "112000")
    private Integer paymentAmount;

    @Schema(description = "사용된 쿠폰 정보 (미사용 시 null)", nullable = true)
    private CouponUsed couponUsed;

    @Schema(description = "주문 일시", example = "2025-10-30T12:00:00")
    private LocalDateTime orderedAt;

    @Getter
    @AllArgsConstructor
    @Schema(description = "주문 항목 정보")
    public static class OrderItem {
        @Schema(description = "주문 항목 ID", example = "1")
        private Long orderItemId;

        @Schema(description = "상품 ID", example = "1")
        private Long itemId;

        @Schema(description = "상품명", example = "기본 티셔츠")
        private String itemName;

        @Schema(description = "상품 단가", example = "29000")
        private Integer price;

        @Schema(description = "주문 수량", example = "2")
        private Integer quantity;

        @Schema(description = "총 가격 (단가 × 수량)", example = "58000")
        private Integer totalPrice;
    }

    @Getter
    @AllArgsConstructor
    @Schema(description = "사용된 쿠폰 정보")
    public static class CouponUsed {
        @Schema(description = "사용자 쿠폰 ID", example = "1")
        private Long userCouponId;

        @Schema(description = "쿠폰명", example = "신규 가입 쿠폰")
        private String couponName;

        @Schema(description = "할인 금액", example = "5000")
        private Integer discountAmount;
    }

    public static CreateOrderResponse of(
            com.side.hhplusecommerce.order.domain.Order order,
            List<com.side.hhplusecommerce.order.domain.OrderItem> orderItems,
            com.side.hhplusecommerce.coupon.domain.Coupon coupon
    ) {
        List<OrderItem> items = orderItems.stream()
                .map(orderItem -> new OrderItem(
                        orderItem.getOrderItemId(),
                        orderItem.getItemId(),
                        orderItem.getName(),
                        orderItem.getPrice(),
                        orderItem.getQuantity(),
                        orderItem.calculateItemTotalPrice()
                ))
                .toList();

        CouponUsed couponUsed = null;
        if (Objects.nonNull(coupon)) {
            couponUsed = new CouponUsed(
                    orderItems.stream()
                            .filter(com.side.hhplusecommerce.order.domain.OrderItem::hasCoupon)
                            .findFirst()
                            .map(com.side.hhplusecommerce.order.domain.OrderItem::getUserCouponId)
                            .orElse(null),
                    coupon.getName(),
                    coupon.getDiscountAmount()
            );
        }

        return new CreateOrderResponse(
                order.getOrderId(),
                items,
                order.getTotalAmount(),
                order.getCouponDiscount(),
                order.getFinalAmount(),
                order.getFinalAmount(),
                couponUsed,
                order.getCreatedAt()
        );
    }
}