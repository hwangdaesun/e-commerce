package com.side.hhplusecommerce.order.domain;

import com.side.hhplusecommerce.cart.domain.CartItem;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.order.exception.InvalidOrderItemItemIdException;
import com.side.hhplusecommerce.order.exception.InvalidOrderItemOrderIdException;
import com.side.hhplusecommerce.order.exception.InvalidOrderItemPriceException;
import com.side.hhplusecommerce.order.exception.InvalidOrderItemQuantityException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "order_items", indexes = {
        @Index(name = "idx_order_items_order_id", columnList = "order_id"),
        @Index(name = "idx_order_items_item_id", columnList = "item_id"),
        @Index(name = "idx_order_items_user_coupon_id", columnList = "user_coupon_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long orderItemId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "user_coupon_id")
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

    /**
     * CartItem과 Item 리스트로부터 OrderItem 리스트를 생성하는 정적 팩토리 메서드
     * @param orderId 주문 ID
     * @param cartItems 장바구니 아이템 리스트
     * @param items 상품 리스트
     * @param userCouponId 사용자 쿠폰 ID (nullable)
     * @return OrderItem 리스트
     */
    public static List<OrderItem> createAll(Long orderId, List<CartItem> cartItems, List<Item> items, Long userCouponId) {
        Map<Long, Item> itemMap = items.stream()
                .collect(Collectors.toMap(Item::getItemId, item -> item));

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            Item item = itemMap.get(cartItem.getItemId());
            if (item != null) {
                OrderItem orderItem = OrderItem.create(
                        orderId,
                        item.getItemId(),
                        item.getName(),
                        item.getPrice(),
                        cartItem.getQuantity(),
                        userCouponId
                );
                orderItems.add(orderItem);
            }
        }
        return orderItems;
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
