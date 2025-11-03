package com.side.hhplusecommerce.order.domain;

import com.side.hhplusecommerce.common.BaseEntity;
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

}
