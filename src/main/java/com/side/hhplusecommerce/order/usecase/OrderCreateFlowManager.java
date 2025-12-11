package com.side.hhplusecommerce.order.usecase;

import com.side.hhplusecommerce.item.service.ItemPopularityService;
import com.side.hhplusecommerce.order.domain.Order;
import com.side.hhplusecommerce.order.domain.OrderItem;
import com.side.hhplusecommerce.order.event.CompensateCouponCommand;
import com.side.hhplusecommerce.order.event.CompensateStockCommand;
import com.side.hhplusecommerce.order.event.CouponFailedEvent;
import com.side.hhplusecommerce.order.event.CouponUsedEvent;
import com.side.hhplusecommerce.order.event.OrderCompletedEvent;
import com.side.hhplusecommerce.order.event.ProcessPaymentEvent;
import com.side.hhplusecommerce.order.event.StockFailedEvent;
import com.side.hhplusecommerce.order.event.StockReservedEvent;
import com.side.hhplusecommerce.order.repository.OrderItemRepository;
import com.side.hhplusecommerce.order.service.ExternalDataPlatformService;
import com.side.hhplusecommerce.order.service.OrderService;
import com.side.hhplusecommerce.payment.service.UserPointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 생성 플로우 매니저
 * 주문 생성 이후의 이벤트 기반 플로우를 관리합니다.
 * - 재고 예약/쿠폰 사용 성공 시 결제 준비 여부 확인 및 결제 진행
 * - 재고/쿠폰/결제 실패 시 보상 트랜잭션 조율
 * - 주문 완료 후처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreateFlowManager {
    private final OrderService orderService;
    private final OrderItemRepository orderItemRepository;
    private final ItemPopularityService itemPopularityService;
    private final ExternalDataPlatformService externalDataPlatformService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserPointService userPointService;

    /**
     * StockReservedEvent 리스너 - 재고 예약 성공 시 플래그 업데이트
     */
    @Async
    @EventListener
    public void handleStockReservedEvent(StockReservedEvent event) {
        log.info("StockReservedEvent received: orderId={}", event.getOrderId());

        // 비관적 락으로 재고 예약 플래그 업데이트 및 결제 준비 여부 확인
        boolean isReadyForPayment = orderService.markStockReservedWithLock(event.getOrderId());

        // 결제 준비 완료 시 결제 이벤트 발행
        if (isReadyForPayment) {
            Order order = orderService.findById(event.getOrderId());
            eventPublisher.publishEvent(ProcessPaymentEvent.from(order));
        }
    }

    /**
     * CouponUsedEvent 리스너 - 쿠폰 사용 성공 시 플래그 업데이트
     */
    @Async
    @EventListener
    public void handleCouponUsedEvent(CouponUsedEvent event) {
        log.info("CouponUsedEvent received: orderId={}", event.getOrderId());

        // 비관적 락으로 쿠폰 사용 플래그 업데이트 및 결제 준비 여부 확인
        boolean isReadyForPayment = orderService.markCouponUsedWithLock(event.getOrderId());

        // 결제 준비 완료 시 결제 이벤트 발행
        if (isReadyForPayment) {
            Order order = orderService.findById(event.getOrderId());
            eventPublisher.publishEvent(ProcessPaymentEvent.from(order));
        }
    }

    /**
     * StockFailedEvent 리스너 - 재고 예약 실패 시 보상 트랜잭션 수행
     */
    @Async
    @EventListener
    public void handleStockFailedEvent(StockFailedEvent event) {
        log.error("StockFailedEvent received: orderId={}, reason={}", event.getOrderId(), event.getReason());

        // 주문 실패 처리
        orderService.failOrder(event.getOrderId(), event.getReason());

        // 쿠폰 복구 (이미 쿠폰을 사용했을 수 있음)
        Order order = orderService.findById(event.getOrderId());
        if (Boolean.TRUE.equals(order.isCouponUsed())) {
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(event.getOrderId());
            Long userCouponId = orderItems.stream()
                    .map(OrderItem::getUserCouponId)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            if (userCouponId != null) {
                eventPublisher.publishEvent(CompensateCouponCommand.of(event.getOrderId(), userCouponId));
            }
        }
    }

    /**
     * CouponFailedEvent 리스너 - 쿠폰 사용 실패 시 보상 트랜잭션 수행
     */
    @Async
    @EventListener
    public void handleCouponFailedEvent(CouponFailedEvent event) {
        log.error("CouponFailedEvent received: orderId={}, reason={}", event.getOrderId(), event.getReason());

        // 주문 실패 처리
        orderService.failOrder(event.getOrderId(), event.getReason());

        // 재고 복구 (이미 재고를 예약했을 수 있음)
        Order order = orderService.findById(event.getOrderId());
        if (order.isStockReserved()) {
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(event.getOrderId());
            Map<Long, Integer> itemQuantities = orderItems.stream()
                    .collect(Collectors.toMap(OrderItem::getItemId, OrderItem::getQuantity));

            eventPublisher.publishEvent(CompensateStockCommand.of(event.getOrderId(), itemQuantities));
        }
    }

    /**
     * ProcessPaymentEvent 리스너 - 결제 처리 (동기)
     */
    @EventListener
    public void handleProcessPaymentEvent(ProcessPaymentEvent event) {
        log.info("ProcessPaymentEvent received: orderId={}", event.getOrderId());

        try {
            // 결제 처리 (동기 - 외부 PG 연동)
            userPointService.use(event.getUserId(), event.getFinalAmount());

            // 결제 성공 처리
            orderService.completeOrderPayment(event.getOrderId());

            // 주문 완료 이벤트 발행
            eventPublisher.publishEvent(OrderCompletedEvent.of(event.getOrderId()));

        } catch (Exception e) {
            log.error("Payment failed: orderId={}", event.getOrderId(), e);

            // 결제 실패 시 보상 트랜잭션
            orderService.failOrder(event.getOrderId(), "결제 실패");

            // 재고 및 쿠폰 복구
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(event.getOrderId());

            // 재고 복구
            Map<Long, Integer> itemQuantities = orderItems.stream()
                    .collect(Collectors.toMap(OrderItem::getItemId, OrderItem::getQuantity));
            eventPublisher.publishEvent(CompensateStockCommand.of(event.getOrderId(), itemQuantities));

            // 쿠폰 복구
            Long userCouponId = orderItems.stream()
                    .map(OrderItem::getUserCouponId)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            if (userCouponId != null) {
                eventPublisher.publishEvent(CompensateCouponCommand.of(event.getOrderId(), userCouponId));
            }
        }
    }

    /**
     * OrderCompletedEvent 리스너 - 주문 완료 후처리 (비동기)
     */
    @Async
    @EventListener
    public void handleOrderCompletedEvent(OrderCompletedEvent event) {
        log.info("OrderCompletedEvent received: orderId={}", event.getOrderId());

        // 후처리 작업
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(event.getOrderId());
        orderItems.forEach(orderItem ->
            itemPopularityService.incrementSalesScore(orderItem.getItemId())
        );

        // 장바구니 삭제는 첫 번째 장바구니 아이템으로 처리 (기존 로직 유지)
        // TODO: cartId를 Order에 저장하거나 다른 방법으로 개선 필요

        externalDataPlatformService.sendOrderDataAsync(event.getOrderId());
    }
}
