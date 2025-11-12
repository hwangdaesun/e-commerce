package com.side.hhplusecommerce.order.domain;

import com.side.hhplusecommerce.common.BaseEntity;
import com.side.hhplusecommerce.order.exception.AlreadyPaidOrderException;
import com.side.hhplusecommerce.order.exception.InvalidCouponDiscountException;
import com.side.hhplusecommerce.order.exception.InvalidOrderAmountException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_user_id", columnList = "user_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    @Column(name = "coupon_discount", nullable = false)
    private Integer couponDiscount;

    @Column(name = "final_amount", nullable = false)
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
        Integer finalAmount = calculateFinalAmount(totalAmount, couponDiscount);

        return Order.builder()
                .orderId(orderId)
                .userId(userId)
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .couponDiscount(couponDiscount)
                .finalAmount(finalAmount)
                .build();
    }

    private static Integer calculateFinalAmount(Integer totalAmount, Integer couponDiscount) {
        Integer result = totalAmount - couponDiscount;
        return Math.max(result, 0);
    }

    private static void validateAmounts(Integer totalAmount, Integer couponDiscount) {
        if (totalAmount < 0) {
            throw new InvalidOrderAmountException();
        }
        if (couponDiscount < 0) {
            throw new InvalidCouponDiscountException();
        }
    }

    public void completePay() {
        if (this.status.equals(OrderStatus.PAID)) {
            throw new AlreadyPaidOrderException();
        }
        this.status = OrderStatus.PAID;
    }

    public void fail() {
        this.status = OrderStatus.FAILED;
    }

}
