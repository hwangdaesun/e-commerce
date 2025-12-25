package com.side.hhplusecommerce.order.infrastructure.kafka.consumer;

import com.side.hhplusecommerce.order.event.*;
import com.side.hhplusecommerce.order.usecase.OrderProcessingManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import static com.side.hhplusecommerce.order.infrastructure.kafka.OrderEventKafkaConstants.*;

/**
 * 주문 플로우 관리 Kafka Consumer
 * OrderCreateFlowManager에서 처리할 모든 이벤트를 수신합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderProcessingConsumer {

    private final OrderProcessingManager orderProcessingManager;

    /**
     * StockReservedEvent 수신
     */
    @KafkaListener(
            topics = TOPIC_STOCK_RESERVED,
            groupId = GROUP_ORDER_FLOW_MANAGER,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeStockReserved(
            @Payload StockReservedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("StockReservedEvent 수신: orderId={}, partition={}, offset={}",
                event.getOrderId(), partition, offset);

        try {
            orderProcessingManager.handleStockReservedEvent(event);
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        } catch (Exception e) {
            log.error("StockReservedEvent 처리 실패: orderId={}", event.getOrderId(), e);
        }
    }

    /**
     * CouponUsedEvent 수신
     */
    @KafkaListener(
            topics = TOPIC_COUPON_USED,
            groupId = GROUP_ORDER_FLOW_MANAGER,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeCouponUsed(
            @Payload CouponUsedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("CouponUsedEvent 수신: orderId={}, partition={}, offset={}",
                event.getOrderId(), partition, offset);

        try {
            orderProcessingManager.handleCouponUsedEvent(event);
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        } catch (Exception e) {
            log.error("CouponUsedEvent 처리 실패: orderId={}", event.getOrderId(), e);
        }
    }

    /**
     * StockFailedEvent 수신
     */
    @KafkaListener(
            topics = TOPIC_STOCK_FAILED,
            groupId = GROUP_ORDER_FLOW_MANAGER,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeStockFailed(
            @Payload StockFailedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("StockFailedEvent 수신: orderId={}, partition={}, offset={}",
                event.getOrderId(), partition, offset);

        try {
            orderProcessingManager.handleStockFailedEvent(event);
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        } catch (Exception e) {
            log.error("StockFailedEvent 처리 실패: orderId={}", event.getOrderId(), e);
        }
    }

    /**
     * CouponFailedEvent 수신
     */
    @KafkaListener(
            topics = TOPIC_COUPON_FAILED,
            groupId = GROUP_ORDER_FLOW_MANAGER,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeCouponFailed(
            @Payload CouponFailedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("CouponFailedEvent 수신: orderId={}, partition={}, offset={}",
                event.getOrderId(), partition, offset);

        try {
            orderProcessingManager.handleCouponFailedEvent(event);
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        } catch (Exception e) {
            log.error("CouponFailedEvent 처리 실패: orderId={}", event.getOrderId(), e);
        }
    }

    /**
     * ProcessPaymentEvent 수신
     */
    @KafkaListener(
            topics = TOPIC_PROCESS_PAYMENT,
            groupId = GROUP_PAYMENT_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeProcessPayment(
            @Payload ProcessPaymentEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("ProcessPaymentEvent 수신: orderId={}, partition={}, offset={}",
                event.getOrderId(), partition, offset);

        try {
            orderProcessingManager.handleProcessPaymentEvent(event);
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        } catch (Exception e) {
            log.error("ProcessPaymentEvent 처리 실패: orderId={}", event.getOrderId(), e);
        }
    }

    /**
     * OrderCompletedEvent 수신
     *
     * 멱등성 보장: 중복 메시지 처리 시에도 안전하게 동작
     * - 이미 처리된 주문은 handleOrderCompletedEvent에서 조기 리턴
     * - 처리 중 예외 발생 시에도 적절한 ACK 처리로 무한 재시도 방지
     */
    @KafkaListener(
            topics = TOPIC_ORDER_COMPLETED,
            groupId = GROUP_POST_PROCESS,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderCompleted(
            @Payload OrderCompletedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("OrderCompletedEvent 수신: orderId={}, partition={}, offset={}",
                event.getOrderId(), partition, offset);

        try {
            orderProcessingManager.handleOrderCompletedEvent(event);
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        } catch (Exception e) {
            log.error("OrderCompletedEvent 처리 실패: orderId={}", event.getOrderId(), e);

            // 예외 발생 시에도 ACK 처리하여 무한 재시도 방지
            // 멱등성이 보장되므로 재처리 시 안전하며,
            // 일시적 장애는 다른 메커니즘(모니터링/알람)으로 대응
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
                log.warn("예외 발생했지만 무한 재시도 방지를 위해 ACK 처리: orderId={}", event.getOrderId());
            }
        }
    }
}
