package com.side.hhplusecommerce.order.domain;

import com.side.hhplusecommerce.common.BaseEntity;
import com.side.hhplusecommerce.order.exception.InvalidCouponDiscountException;
import com.side.hhplusecommerce.order.exception.InvalidOrderAmountException;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class Order extends BaseEntity {
    private Long orderId;
    private Long userId;
    private OrderStatus status;
    private Integer totalAmount;
    private Integer couponDiscount;
    private Integer finalAmount;

    @Builder(access = AccessLevel.PRIVATE)
    private Order(Long orderId, Long userId, OrderStatus status, Integer totalAmount, Integer couponDiscount, Integer finalAmount) {
        super();
        this.orderId = orderId;
        this.userId = userId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.couponDiscount = couponDiscount;
        this.finalAmount = finalAmount;
    }

    public static Order create(Long orderId, Long userId, Integer totalAmount, Integer couponDiscount) {
        validateAmounts(totalAmount, couponDiscount);
        Integer finalAmount = totalAmount - couponDiscount;

        return Order.builder()
                .orderId(orderId)
                .userId(userId)
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .couponDiscount(couponDiscount)
                .finalAmount(finalAmount)
                .build();
    }

    private static void validateAmounts(Integer totalAmount, Integer couponDiscount) {
        if (totalAmount < 0) {
            throw new InvalidOrderAmountException();
        }
        if (couponDiscount < 0) {
            throw new InvalidCouponDiscountException();
        }
    }

}
