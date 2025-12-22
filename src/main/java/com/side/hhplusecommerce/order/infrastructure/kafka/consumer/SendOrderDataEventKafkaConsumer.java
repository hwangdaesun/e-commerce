package com.side.hhplusecommerce.order.infrastructure.kafka.consumer;

import com.side.hhplusecommerce.order.event.SendOrderDataEvent;
import com.side.hhplusecommerce.order.service.ExternalDataPlatformService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.side.hhplusecommerce.order.infrastructure.kafka.OrderEventKafkaConstants.GROUP_EXTERNAL_DATA_SERVICE;
import static com.side.hhplusecommerce.order.infrastructure.kafka.OrderEventKafkaConstants.TOPIC_SEND_ORDER_DATA;

/**
 * SendOrderDataEvent Kafka Consumer
 * 주문 완료 후 외부 데이터 플랫폼으로 주문 데이터를 전송하는 Consumer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SendOrderDataEventKafkaConsumer {

    private final ExternalDataPlatformService externalDataPlatformService;

    @KafkaListener(
            topics = TOPIC_SEND_ORDER_DATA,
            groupId = GROUP_EXTERNAL_DATA_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleSendOrderDataEvent(SendOrderDataEvent event) {
        log.info("SendOrderDataEvent received: orderId={}", event.getOrderId());

        try {
            // 외부 데이터 플랫폼으로 주문 데이터 전송 (비동기)
            externalDataPlatformService.sendOrderDataAsync(event.getOrderId());
            log.info("외부 데이터 플랫폼 전송 완료: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("외부 데이터 플랫폼 전송 실패: orderId={}", event.getOrderId(), e);
            // TODO: 재시도 로직 또는 Dead Letter Queue 처리
        }
    }
}