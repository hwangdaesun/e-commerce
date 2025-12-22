package com.side.hhplusecommerce.order.infrastructure.kafka.consumer;

import com.side.hhplusecommerce.coupon.service.CouponService;
import com.side.hhplusecommerce.order.event.CompensateCouponCommand;
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
 * CompensateCouponCommand Kafka Consumer
 * 쿠폰 보상 트랜잭션을 처리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompensateCouponCommandKafkaConsumer {

    private final CouponService couponService;

    @KafkaListener(
            topics = TOPIC_COMPENSATE_COUPON,
            groupId = GROUP_COUPON_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload CompensateCouponCommand command,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("CompensateCouponCommand 수신: orderId={}, partition={}, offset={}",
                command.getOrderId(), partition, offset);

        try {
            // 쿠폰 복구 처리
            couponService.handleCompensateCouponCommand(command);

            // Kafka 커밋
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

            log.info("CompensateCouponCommand 처리 완료: orderId={}", command.getOrderId());

        } catch (Exception e) {
            log.error("CompensateCouponCommand 처리 실패: orderId={}", command.getOrderId(), e);
            // 실패 시 ACK하지 않음 -> 재처리
        }
    }
}
