package com.side.hhplusecommerce.coupon.infrastructure.redis;

import com.side.hhplusecommerce.coupon.service.CouponIssueTransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.side.hhplusecommerce.coupon.infrastructure.redis.CouponIssueQueueConstants.*;

/**
 * Redis Stream Consumer
 * Redis Stream에서 쿠폰 발급 요청을 가져와서 처리합니다.
 *
 * 처리 흐름:
 * 1. Stream에서 메시지 읽기 (Consumer Group 사용)
 * 2. MySQL에서 쿠폰 발급 처리 (CouponIssueTransactionService)
 * 3. MySQL 저장 성공 후:
 *    - Redis Set에 사용자 추가 (중복 발급 방지용)
 *    - Redis 재고 차감 (필터링용)
 * 4. Stream 메시지 ACK 처리
 */
@Slf4j
@Component
public class CouponIssueQueueConsumer {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CouponIssueTransactionService couponIssueTransactionService;
    private final CouponRedisStockService couponRedisStockService;

    private StreamMessageListenerContainer<String, MapRecord<String, Object, Object>> listenerContainer;

    /**
     * 인스턴스별 고정 Consumer Name
     * 형식: coupon-consumer-{pid}@{hostname}
     */
    private final String consumerName;

    /**
     * Consumer Name 초기화
     */
    public CouponIssueQueueConsumer(
            RedisTemplate<String, Object> redisTemplate,
            CouponIssueTransactionService couponIssueTransactionService,
            CouponRedisStockService couponRedisStockService) {
        this.redisTemplate = redisTemplate;
        this.couponIssueTransactionService = couponIssueTransactionService;
        this.couponRedisStockService = couponRedisStockService;

        // 인스턴스별 고유한 Consumer Name 생성 (pid@hostname)
        this.consumerName = CONSUMER_PREFIX + ManagementFactory.getRuntimeMXBean().getName();
        log.info("Consumer Name 초기화: {}", consumerName);
    }

    /**
     * Consumer Group 초기화 및 리스너 시작
     */
    @PostConstruct
    public void init() {
        // Consumer Group이 없으면 생성
        createConsumerGroupIfNotExists();
    }

    /**
     * Consumer Group 생성
     */
    private void createConsumerGroupIfNotExists() {
        try {
            // Consumer Group 정보 조회
            StreamInfo.XInfoGroups groups = redisTemplate.opsForStream()
                    .groups(COUPON_ISSUE_QUEUE);

            boolean groupExists = groups.stream()
                    .anyMatch(group -> CONSUMER_GROUP.equals(group.groupName()));

            if (!groupExists) {
                redisTemplate.opsForStream().createGroup(COUPON_ISSUE_QUEUE, CONSUMER_GROUP);
                log.info("Consumer Group 생성: {}", CONSUMER_GROUP);
            } else {
                log.info("Consumer Group 이미 존재: {}", CONSUMER_GROUP);
            }
        } catch (Exception e) {
            // Stream이 없으면 생성 후 Consumer Group 생성
            try {
                redisTemplate.opsForStream().createGroup(COUPON_ISSUE_QUEUE, ReadOffset.from("0"), CONSUMER_GROUP);
                log.info("Stream 및 Consumer Group 생성: {}", CONSUMER_GROUP);
            } catch (Exception ex) {
                log.error("Consumer Group 생성 실패", ex);
            }
        }
    }

    /**
     * 배치로 메시지를 읽어서 처리
     * Scheduler에서 주기적으로 호출됩니다.
     *
     * @return 처리된 메시지 수
     */
    public int processBatch() {
        try {
            // Consumer Group에서 메시지 읽기 (최대 BATCH_SIZE개)
            List<MapRecord<String, Object, Object>> messages = redisTemplate.opsForStream().read(
                    Consumer.from(CONSUMER_GROUP, consumerName),
                    StreamReadOptions.empty().count(BATCH_SIZE).block(Duration.ofMillis(CONSUMER_TIMEOUT_MS)),
                    StreamOffset.create(COUPON_ISSUE_QUEUE, ReadOffset.lastConsumed())
            );

            if (messages == null || messages.isEmpty()) {
                log.debug("처리할 메시지 없음");
                return 0;
            }

            int processedCount = 0;
            for (MapRecord<String, Object, Object> message : messages) {
                try {
                    processMessage(message);
                    processedCount++;
                } catch (Exception e) {
                    log.error("메시지 처리 실패: messageId={}", message.getId(), e);
                    // 실패한 메시지는 ACK하지 않음 (재처리 대상)
                }
            }

            log.info("배치 처리 완료: processedCount={}/{}", processedCount, messages.size());
            return processedCount;

        } catch (Exception e) {
            log.error("배치 처리 중 오류 발생", e);
            return 0;
        }
    }

    /**
     * 개별 메시지 처리
     */
    private void processMessage(MapRecord<String, Object, Object> message) {
        RecordId messageId = message.getId();
        Map<Object, Object> messageData = message.getValue();

        Long userId = Long.parseLong((String) messageData.get(FIELD_USER_ID));
        Long couponId = Long.parseLong((String) messageData.get(FIELD_COUPON_ID));

        log.info("메시지 처리 시작: messageId={}, couponId={}, userId={}", messageId, couponId, userId);

        try {
            // MySQL에서 쿠폰 발급 처리 (트랜잭션)
            couponIssueTransactionService.issueCoupon(couponId, userId);

            // MySQL 저장 성공 후 Redis 업데이트
            couponRedisStockService.addIssuedUser(couponId, userId);
            couponRedisStockService.decreaseStock(couponId);

            // 메시지 ACK 처리
            redisTemplate.opsForStream().acknowledge(COUPON_ISSUE_QUEUE, CONSUMER_GROUP, messageId);

            log.info("메시지 처리 성공: messageId={}, couponId={}, userId={}", messageId, couponId, userId);

        } catch (Exception e) {
            log.error("쿠폰 발급 실패: messageId={}, couponId={}, userId={}", messageId, couponId, userId, e);
            // 실패 시 ACK하지 않음 -> 재처리 대상
            throw e;
        }
    }

    /**
     * Pending 메시지 처리 (재처리)
     * 현재 Consumer에게 할당된 ACK되지 않은 메시지를 재처리합니다.
     *
     * @return 처리된 메시지 수
     */
    public int processPendingMessages() {
        try {
            // 현재 Consumer의 Pending 메시지 조회
            PendingMessages pendingMessages = redisTemplate.opsForStream().pending(
                    COUPON_ISSUE_QUEUE,
                    Consumer.from(CONSUMER_GROUP, consumerName),
                    Range.unbounded(),
                    BATCH_SIZE
            );

            if (pendingMessages == null || pendingMessages.isEmpty()) {
                log.debug("Pending 메시지 없음 (Consumer: {})", consumerName);
                return 0;
            }

            log.info("Pending 메시지 발견: count={}, consumer={}", pendingMessages.size(), consumerName);

            int processedCount = 0;
            for (PendingMessage pendingMessage : pendingMessages) {
                try {
                    // XCLAIM을 사용하여 Pending 메시지를 다시 읽어옴
                    List<MapRecord<String, Object, Object>> messages = redisTemplate.opsForStream().read(
                            Consumer.from(CONSUMER_GROUP, consumerName),
                            StreamReadOptions.empty().count(1),
                            StreamOffset.create(COUPON_ISSUE_QUEUE, ReadOffset.from(pendingMessage.getIdAsString()))
                    );

                    if (messages != null && !messages.isEmpty()) {
                        processMessage(messages.get(0));
                        processedCount++;
                    }
                } catch (Exception e) {
                    log.error("Pending 메시지 처리 실패: messageId={}", pendingMessage.getIdAsString(), e);
                }
            }

            log.info("Pending 메시지 처리 완료: processedCount={}, consumer={}", processedCount, consumerName);
            return processedCount;

        } catch (Exception e) {
            log.error("Pending 메시지 처리 중 오류 발생", e);
            return 0;
        }
    }

    @PreDestroy
    public void destroy() {
        if (listenerContainer != null) {
            listenerContainer.stop();
            log.info("StreamMessageListenerContainer 종료");
        }
    }
}