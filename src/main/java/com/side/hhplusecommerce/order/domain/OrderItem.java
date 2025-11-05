package com.side.hhplusecommerce.order.domain;

import com.side.hhplusecommerce.order.exception.InvalidOrderItemItemIdException;
import com.side.hhplusecommerce.order.exception.InvalidOrderItemOrderIdException;
import com.side.hhplusecommerce.order.exception.InvalidOrderItemPriceException;
import com.side.hhplusecommerce.order.exception.InvalidOrderItemQuantityException;
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
        validateQuantity(quantity);
        validatePrice(price);
        return OrderItem.builder()
                .orderId(orderId)
                .itemId(itemId)
                .name(name)
                .price(price)
                .quantity(quantity)
                .userCouponId(userCouponId)
                .build();
    }

    public static OrderItem createWithId(Long orderItemId, Long orderId, Long itemId, String name, Integer price, Integer quantity, Long userCouponId) {
        validateReferentialIntegrity(orderId, itemId);
        validateQuantity(quantity);
        validatePrice(price);
        return OrderItem.builder()
                .orderItemId(orderItemId)
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

    private static void validateQuantity(Integer quantity) {
        if (Objects.isNull(quantity) || quantity < 1) {
            throw new InvalidOrderItemQuantityException();
        }
    }

    private static void validatePrice(Integer price) {
        if (Objects.isNull(price) || price < 0) {
            throw new InvalidOrderItemPriceException();
        }
    }

    // 쿠폰 적용 전
    public Integer calculateItemTotalPrice() {
        return price * quantity;
    }

    public boolean hasCoupon() {
        return Objects.nonNull(userCouponId);
    }

}
