package com.side.hhplusecommerce.order.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 주문 이벤트 Kafka Producer
 * 모든 주문 관련 이벤트를 Kafka에 발행합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 이벤트를 Kafka에 발행
     *
     * @param topic 토픽 이름
     * @param key 메시지 키 (이벤트 ID)
     * @param event 이벤트 객체
     * @param <T> 이벤트 타입
     */
    public <T> void publish(String topic, String key, T event) {
        try {
            kafkaTemplate.send(topic, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Kafka 이벤트 전송 실패: topic={}, key={}", topic, key, ex);
                        } else {
                            log.info("Kafka 이벤트 전송 성공: topic={}, key={}, partition={}, offset={}",
                                    topic, key,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception e) {
            log.error("Kafka 이벤트 전송 중 오류 발생: topic={}, key={}", topic, key, e);
            throw new RuntimeException("이벤트 발행 실패", e);
        }
    }
}