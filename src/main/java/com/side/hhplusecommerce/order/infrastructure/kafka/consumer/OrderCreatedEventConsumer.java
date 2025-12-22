package com.side.hhplusecommerce.order.infrastructure.kafka.consumer;

import com.side.hhplusecommerce.coupon.service.CouponService;
import com.side.hhplusecommerce.item.service.ItemStockService;
import com.side.hhplusecommerce.order.event.OrderCreatedEvent;
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
 * OrderCreatedEvent Kafka Consumer (재고 예약용)
 * ItemStockService에서 재고를 예약합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedEventConsumer {

    private final ItemStockService itemStockService;
    private final CouponService couponService;

    @KafkaListener(
            topics = TOPIC_ORDER_CREATED,
            groupId = GROUP_COUPON_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeForCoupon(
            @Payload OrderCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("OrderCreatedEvent 수신 (쿠폰 사용): orderId={}, partition={}, offset={}",
                event.getOrderId(), partition, offset);

        try {
            // 쿠폰 사용 처리 (비동기)
            couponService.handleOrderCreatedEvent(event);

            // Kafka 커밋
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

            log.info("OrderCreatedEvent 처리 완료 (쿠폰 사용): orderId={}", event.getOrderId());

        } catch (Exception e) {
            log.error("OrderCreatedEvent 처리 실패 (쿠폰 사용): orderId={}", event.getOrderId(), e);
            // 실패 시 ACK하지 않음 -> 재처리
        }
    }

    @KafkaListener(
            topics = TOPIC_ORDER_CREATED,
            groupId = GROUP_STOCK_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeForStock(
            @Payload OrderCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("OrderCreatedEvent 수신 (재고 예약): orderId={}, partition={}, offset={}",
                event.getOrderId(), partition, offset);

        try {
            // 재고 예약 처리 (비동기)
            itemStockService.handleOrderCreatedEvent(event);

            // Kafka 커밋
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

            log.info("OrderCreatedEvent 처리 완료 (재고 예약): orderId={}", event.getOrderId());

        } catch (Exception e) {
            log.error("OrderCreatedEvent 처리 실패 (재고 예약): orderId={}", event.getOrderId(), e);
            // 실패 시 ACK하지 않음 -> 재처리
        }
    }
}
