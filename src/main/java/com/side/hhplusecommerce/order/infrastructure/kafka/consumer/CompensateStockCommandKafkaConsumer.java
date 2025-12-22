package com.side.hhplusecommerce.order.infrastructure.kafka.consumer;

import com.side.hhplusecommerce.item.service.ItemStockService;
import com.side.hhplusecommerce.order.event.CompensateStockCommand;
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
 * CompensateStockCommand Kafka Consumer
 * 재고 보상 트랜잭션을 처리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompensateStockCommandKafkaConsumer {

    private final ItemStockService itemStockService;

    @KafkaListener(
            topics = TOPIC_COMPENSATE_STOCK,
            groupId = GROUP_STOCK_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload CompensateStockCommand command,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("CompensateStockCommand 수신: orderId={}, partition={}, offset={}",
                command.getOrderId(), partition, offset);

        try {
            // 재고 복구 처리
            itemStockService.handleCompensateStockCommand(command);

            // Kafka 커밋
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

            log.info("CompensateStockCommand 처리 완료: orderId={}", command.getOrderId());

        } catch (Exception e) {
            log.error("CompensateStockCommand 처리 실패: orderId={}", command.getOrderId(), e);
            // 실패 시 ACK하지 않음 -> 재처리
        }
    }
}
