package com.side.hhplusecommerce.order.usecase;

import com.side.hhplusecommerce.item.service.ItemPopularityService;
import com.side.hhplusecommerce.order.domain.Order;
import com.side.hhplusecommerce.order.domain.OrderItem;
import com.side.hhplusecommerce.order.domain.OrderStatus;
import com.side.hhplusecommerce.order.event.CompensateCouponCommand;
import com.side.hhplusecommerce.order.event.CompensateStockCommand;
import com.side.hhplusecommerce.order.event.CouponFailedEvent;
import com.side.hhplusecommerce.order.event.CouponUsedEvent;
import com.side.hhplusecommerce.order.event.OrderCompletedEvent;
import com.side.hhplusecommerce.order.event.ProcessPaymentEvent;
import com.side.hhplusecommerce.order.event.SendOrderDataEvent;
import com.side.hhplusecommerce.order.event.StockFailedEvent;
import com.side.hhplusecommerce.order.event.StockReservedEvent;
import com.side.hhplusecommerce.order.infrastructure.kafka.OrderEventKafkaProducer;
import com.side.hhplusecommerce.order.infrastructure.redis.OrderEventTracker;
import com.side.hhplusecommerce.order.repository.OrderItemRepository;
import com.side.hhplusecommerce.order.service.ExternalDataPlatformService;
import com.side.hhplusecommerce.order.service.OrderService;
import com.side.hhplusecommerce.payment.service.UserPointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.side.hhplusecommerce.order.infrastructure.kafka.OrderEventKafkaConstants.*;

/**
 * 주문 생성 플로우 매니저
 * 주문 생성 이후의 Kafka 이벤트 기반 플로우를 관리합니다.
 * - 재고 예약/쿠폰 사용 성공 시 결제 준비 여부 확인 및 결제 진행
 * - 재고/쿠폰/결제 실패 시 보상 트랜잭션 조율
 * - 주문 완료 후처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderProcessingManager {
    private final OrderService orderService;
    private final OrderItemRepository orderItemRepository;
    private final ItemPopularityService itemPopularityService;
    private final ExternalDataPlatformService externalDataPlatformService;
    private final OrderEventKafkaProducer kafkaProducer;
    private final UserPointService userPointService;
    private final OrderEventTracker orderEventTracker;

    /**
     * StockReservedEvent 처리 - 재고 예약 성공 시 Redis 플래그 업데이트
     */
    public void handleStockReservedEvent(StockReservedEvent event) {
        log.info("StockReservedEvent received: orderId={}", event.getOrderId());

        // Redis에 재고 예약 플래그 업데이트 및 결제 준비 여부 확인
        boolean isReadyForPayment = orderEventTracker.markStockReserved(event.getOrderId());

        // 결제 준비 완료 시 결제 이벤트 발행 (Kafka)
        if (isReadyForPayment) {
            Order order = orderService.findById(event.getOrderId());
            ProcessPaymentEvent paymentEvent = ProcessPaymentEvent.from(order);
            kafkaProducer.publish(TOPIC_PROCESS_PAYMENT, event.getOrderId().toString(), paymentEvent);

            log.info("재고/쿠폰 모두 완료, ProcessPaymentEvent 발행: orderId={}", event.getOrderId());
        }
    }

    /**
     * CouponUsedEvent 처리 - 쿠폰 사용 성공 시 Redis 플래그 업데이트
     */
    public void handleCouponUsedEvent(CouponUsedEvent event) {
        log.info("CouponUsedEvent received: orderId={}", event.getOrderId());

        // Redis에 쿠폰 사용 플래그 업데이트 및 결제 준비 여부 확인
        boolean isReadyForPayment = orderEventTracker.markCouponUsed(event.getOrderId());

        // 결제 준비 완료 시 결제 이벤트 발행 (Kafka)
        if (isReadyForPayment) {
            Order order = orderService.findById(event.getOrderId());
            ProcessPaymentEvent paymentEvent = ProcessPaymentEvent.from(order);
            kafkaProducer.publish(TOPIC_PROCESS_PAYMENT, event.getOrderId().toString(), paymentEvent);

            log.info("재고/쿠폰 모두 완료, ProcessPaymentEvent 발행: orderId={}", event.getOrderId());
        }
    }

    /**
     * StockFailedEvent 처리 - 재고 예약 실패 시 보상 트랜잭션 수행
     */
    public void handleStockFailedEvent(StockFailedEvent event) {
        log.error("StockFailedEvent received: orderId={}, reason={}", event.getOrderId(), event.getReason());

        // 주문 실패 처리
        orderService.failOrder(event.getOrderId(), event.getReason());

        // 쿠폰 복구 (Redis에서 쿠폰 사용 여부 확인)
        if (orderEventTracker.isCouponUsed(event.getOrderId())) {
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(event.getOrderId());
            Long userCouponId = orderItems.stream()
                    .map(OrderItem::getUserCouponId)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            if (userCouponId != null) {
                CompensateCouponCommand command = CompensateCouponCommand.of(event.getOrderId(), userCouponId);
                kafkaProducer.publish(TOPIC_COMPENSATE_COUPON, event.getOrderId().toString(), command);
            }
        }
    }

    /**
     * CouponFailedEvent 처리 - 쿠폰 사용 실패 시 보상 트랜잭션 수행
     */
    public void handleCouponFailedEvent(CouponFailedEvent event) {
        log.error("CouponFailedEvent received: orderId={}, reason={}", event.getOrderId(), event.getReason());

        // 주문 실패 처리
        orderService.failOrder(event.getOrderId(), event.getReason());

        // 재고 복구 (Redis에서 재고 예약 여부 확인)
        if (orderEventTracker.isStockReserved(event.getOrderId())) {
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(event.getOrderId());
            Map<Long, Integer> itemQuantities = orderItems.stream()
                    .collect(Collectors.toMap(OrderItem::getItemId, OrderItem::getQuantity));

            CompensateStockCommand command = CompensateStockCommand.of(event.getOrderId(), itemQuantities);
            kafkaProducer.publish(TOPIC_COMPENSATE_STOCK, event.getOrderId().toString(), command);
        }
    }

    /**
     * ProcessPaymentEvent 처리 - 결제 처리 (동기)
     */
    public void handleProcessPaymentEvent(ProcessPaymentEvent event) {
        log.info("ProcessPaymentEvent received: orderId={}", event.getOrderId());

        try {
            // 결제 처리 (동기 - 외부 PG 연동)
            userPointService.use(event.getUserId(), event.getFinalAmount());

            // 결제 성공 처리
            orderService.completeOrderPayment(event.getOrderId());

            // 주문 완료 이벤트 발행 (Kafka)
            OrderCompletedEvent completedEvent = OrderCompletedEvent.of(event.getOrderId());
            kafkaProducer.publish(TOPIC_ORDER_COMPLETED, event.getOrderId().toString(), completedEvent);

        } catch (Exception e) {
            log.error("Payment failed: orderId={}", event.getOrderId(), e);

            // 결제 실패 시 보상 트랜잭션
            orderService.failOrder(event.getOrderId(), "결제 실패");

            // 재고 및 쿠폰 복구
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(event.getOrderId());

            // 재고 복구 (Kafka)
            Map<Long, Integer> itemQuantities = orderItems.stream()
                    .collect(Collectors.toMap(OrderItem::getItemId, OrderItem::getQuantity));
            CompensateStockCommand stockCommand = CompensateStockCommand.of(event.getOrderId(), itemQuantities);
            kafkaProducer.publish(TOPIC_COMPENSATE_STOCK, event.getOrderId().toString(), stockCommand);

            // 쿠폰 복구 (Kafka)
            Long userCouponId = orderItems.stream()
                    .map(OrderItem::getUserCouponId)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            if (userCouponId != null) {
                CompensateCouponCommand couponCommand = CompensateCouponCommand.of(event.getOrderId(), userCouponId);
                kafkaProducer.publish(TOPIC_COMPENSATE_COUPON, event.getOrderId().toString(), couponCommand);
            }

            // Redis 이벤트 추적 데이터 삭제
            orderEventTracker.delete(event.getOrderId());
        }
    }

    /**
     * OrderCompletedEvent 처리 - 주문 완료 후처리 (비동기)
     *
     * 멱등성 보장: 중복 이벤트 처리 시에도 안전하게 동작
     */
    public void handleOrderCompletedEvent(OrderCompletedEvent event) {
        log.info("OrderCompletedEvent received: orderId={}", event.getOrderId());

        Order order = orderService.findById(event.getOrderId());

        // 중복 처리 방지: 이미 PAID 상태면 후처리 작업 스킵
        if (order.getStatus() == OrderStatus.PAID) {
            log.info("Already processed order, skipping: orderId={}", event.getOrderId());
            return;
        }

        // 주문 상태 업데이트
        order.completePay();

        // 후처리 작업: 인기 상품 점수 증가 (한 번만 실행됨)
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(event.getOrderId());
        orderItems.forEach(orderItem ->
            itemPopularityService.incrementSalesScore(orderItem.getItemId())
        );

        // 외부 데이터 플랫폼 전송 이벤트 발행 (Kafka) (한 번만 실행됨)
        SendOrderDataEvent sendDataEvent = SendOrderDataEvent.of(event.getOrderId());
        kafkaProducer.publish(TOPIC_SEND_ORDER_DATA, event.getOrderId().toString(), sendDataEvent);
        log.info("SendOrderDataEvent 발행: orderId={}", event.getOrderId());

        // Redis 이벤트 추적 데이터 삭제 (주문 완료 시)
        orderEventTracker.delete(event.getOrderId());
    }
}
