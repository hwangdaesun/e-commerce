package com.side.hhplusecommerce.order.usecase;

import com.side.hhplusecommerce.cart.domain.CartItem;
import com.side.hhplusecommerce.cart.domain.CartItemValidator;
import com.side.hhplusecommerce.cart.service.CartItemService;
import com.side.hhplusecommerce.coupon.domain.Coupon;
import com.side.hhplusecommerce.coupon.service.CouponService;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.domain.ItemValidator;
import com.side.hhplusecommerce.order.controller.dto.CreateOrderResponse;
import com.side.hhplusecommerce.order.event.OrderCreatedEvent;
import com.side.hhplusecommerce.order.infrastructure.kafka.OrderEventKafkaProducer;
import com.side.hhplusecommerce.order.infrastructure.redis.OrderEventTracker;
import com.side.hhplusecommerce.order.service.OrderService;
import com.side.hhplusecommerce.order.service.dto.OrderCreateResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.side.hhplusecommerce.order.infrastructure.kafka.OrderEventKafkaConstants.*;

/**
 * 주문 생성 유스케이스
 * 주문 생성 진입점이며, 검증 및 주문 생성 후 이벤트를 Kafka로 발행합니다.
 * 이후의 플로우는 OrderCreateFlowManager가 관리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreateUseCase {
    private final CartItemValidator cartItemValidator;
    private final CartItemService cartItemService;
    private final ItemValidator itemValidator;
    private final CouponService couponService;
    private final OrderService orderService;
    private final OrderEventKafkaProducer kafkaProducer;
    private final OrderEventTracker orderEventTracker;

    /**
     * 주문 생성 진입점 - 검증 후 주문을 생성하고 OrderCreatedEvent 발행
     */
    public CreateOrderResponse create(Long userId, List<Long> cartItemIds, Long userCouponId) {
        // 1. 검증 단계 (트랜잭션 없음 - 읽기만 수행)
        List<CartItem> validCartItems = cartItemValidator.validateOwnership(userId, cartItemIds);
        List<Item> items = itemValidator.validateExistence(getItemIds(validCartItems));
        Integer totalAmount = cartItemService.calculateTotalAmount(validCartItems, items);

        // 쿠폰 정보 조회 (쿠폰 검증 및 할인 금액 계산, 실제 사용은 이벤트에서 처리)
        Integer couponDiscount = 0;
        Coupon coupon = null;
        if (userCouponId != null) {
            couponDiscount = couponService.calculateDiscount(userCouponId, totalAmount);
            coupon = couponService.getCouponByUserCouponId(userCouponId);
        }

        // 2. 주문 생성 (동기)
        OrderCreateResult orderCreateResult = orderService.createOrder(
                userId, validCartItems, items, totalAmount, couponDiscount, userCouponId);

        // 3. Redis에 이벤트 추적 상태 초기화 (재고: false, 쿠폰: false 또는 없으면 true)
        orderEventTracker.initialize(orderCreateResult.getOrderId(), userCouponId != null);

        // 4. OrderCreatedEvent 발행 (Kafka - ItemStockService와 CouponService가 리슨)
        OrderCreatedEvent event = OrderCreatedEvent.of(
                orderCreateResult.getOrderId(),
                userId,
                validCartItems,
                items,
                userCouponId
        );
        kafkaProducer.publish(TOPIC_ORDER_CREATED, event.getOrderId().toString(), event);

        log.info("OrderCreatedEvent Kafka 발행: orderId={}", event.getOrderId());

        return CreateOrderResponse.of(orderCreateResult, coupon);
    }

    private List<Long> getItemIds(List<CartItem> validCartItems) {
        return validCartItems.stream()
                .map(CartItem::getItemId)
                .toList();
    }
}
