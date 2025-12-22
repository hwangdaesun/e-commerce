package com.side.hhplusecommerce.coupon.infrastructure.kafka;

import com.side.hhplusecommerce.coupon.infrastructure.redis.dto.CouponIssueMessage;
import com.side.hhplusecommerce.coupon.service.CouponIssueTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import static com.side.hhplusecommerce.coupon.infrastructure.kafka.CouponIssueKafkaConstants.*;

/**
 * 쿠폰 발급 Kafka Consumer
 * Kafka에서 쿠폰 발급 메시지를 읽어 처리합니다.
 *
 * 처리 흐름:
 * 1. Kafka에서 메시지 읽기 (파티션당 1개의 컨슈머)
 * 2. MySQL에서 쿠폰 발급 처리 (CouponIssueTransactionService)
 * 3. MySQL 저장 성공 후 Redis Set에 사용자 추가 (중복 발급 방지용)
 * 4. Kafka 메시지 커밋 (ACK)
 * 5. 실패 시 커밋하지 않음 (재처리 대상)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueConsumer {

    private final CouponIssueTransactionService couponIssueTransactionService;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 쿠폰 발급 메시지 처리
     *
     * @param message 쿠폰 발급 메시지
     * @param partition 파티션 번호
     * @param offset 오프셋
     * @param acknowledgment ACK 객체
     */
    @KafkaListener(
            topics = TOPIC_COUPON_ISSUE,
            groupId = CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listen(
            @Payload CouponIssueMessage message,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        Long couponId = message.getCouponId();
        Long userId = message.getUserId();

        log.info("쿠폰 발급 메시지 수신: couponId={}, userId={}, partition={}, offset={}",
                couponId, userId, partition, offset);

        try {
            // MySQL에서 쿠폰 발급 처리 (트랜잭션)
            couponIssueTransactionService.issueCoupon(couponId, userId);

            // MySQL 저장 성공 후 Redis Set에 사용자 추가 (중복 발급 방지용)
            addIssuedUser(couponId, userId);

            // Kafka 메시지 커밋 (ACK)
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

            log.info("쿠폰 발급 성공 및 커밋: couponId={}, userId={}, partition={}, offset={}",
                    couponId, userId, partition, offset);

        } catch (Exception e) {
            log.error("쿠폰 발급 실패 (재처리 대상): couponId={}, userId={}, partition={}, offset={}",
                    couponId, userId, partition, offset, e);
            // 실패 시 ACK하지 않음 -> Kafka에서 재처리
            // acknowledgment.acknowledge()를 호출하지 않으면 메시지가 커밋되지 않음
        }
    }

    /**
     * Redis Set에 발급된 사용자 추가 (중복 발급 방지용)
     * Consumer에서 MySQL 저장 성공 후 호출됩니다.
     *
     * @param couponId 쿠폰 ID
     * @param userId 사용자 ID
     */
    private void addIssuedUser(Long couponId, Long userId) {
        String issuedUsersKey = COUPON_ISSUED_USERS_PREFIX + couponId;
        redisTemplate.opsForSet().add(issuedUsersKey, userId.toString());
        log.debug("Redis Set에 발급 사용자 추가: couponId={}, userId={}", couponId, userId);
    }
}
