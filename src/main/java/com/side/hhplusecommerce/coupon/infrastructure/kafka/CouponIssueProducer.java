package com.side.hhplusecommerce.coupon.infrastructure.kafka;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.coupon.infrastructure.redis.dto.CouponIssueMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static com.side.hhplusecommerce.coupon.infrastructure.kafka.CouponIssueKafkaConstants.*;

/**
 * 쿠폰 발급 Kafka Producer
 * Kafka에 쿠폰 발급 요청을 전송합니다.
 *
 * 역할:
 * 1. Redis Set 중복 발급 확인 (1차 필터링 - 조회만)
 * 2. Redis SET 크기 확인으로 재고 필터링 (2차 필터링)
 * 3. Kafka에 메시지 전송 (couponId 기준 파티셔닝)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueProducer {

    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, CouponIssueMessage> kafkaTemplate;

    /**
     * 쿠폰 발급 요청을 Kafka에 전송
     *
     * @param couponId 쿠폰 ID
     * @param userId 사용자 ID
     * @param remainingQuantity 쿠폰 남은 재고 수량 (CouponStock)
     * @return 성공 여부
     */
    public boolean enqueue(Long couponId, Long userId, Integer remainingQuantity) {
        // 1. Redis Set 중복 발급 확인 (1차 필터링 - 조회만)
        if (isAlreadyIssued(couponId, userId)) {
            log.warn("이미 발급된 쿠폰 (Redis 필터링): couponId={}, userId={}", couponId, userId);
            throw new CustomException(ErrorCode.ALREADY_ISSUED_COUPON);
        }

        // 2. Redis SET 크기로 재고 확인 (2차 필터링)
        Long issuedCount = getIssuedCount(couponId);
        if (issuedCount >= remainingQuantity) {
            log.warn("쿠폰 재고 부족 (Redis 필터링): couponId={}, issuedCount={}, remainingQuantity={}",
                    couponId, issuedCount, remainingQuantity);
            throw new CustomException(ErrorCode.COUPON_SOLD_OUT);
        }

        // 3. Kafka에 메시지 전송 (couponId를 key로 사용하여 파티셔닝)
        try {
            CouponIssueMessage message = CouponIssueMessage.of(userId, couponId);

            // couponId를 key로 사용하여 같은 쿠폰은 같은 파티션으로 전송 (선착순 보장)
            kafkaTemplate.send(TOPIC_COUPON_ISSUE, couponId.toString(), message)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Kafka 메시지 전송 실패: couponId={}, userId={}", couponId, userId, ex);
                        } else {
                            log.info("Kafka 메시지 전송 성공: couponId={}, userId={}, partition={}, offset={}",
                                    couponId, userId,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });

            log.info("쿠폰 발급 요청 Kafka 전송: couponId={}, userId={}, issuedCount={}",
                    couponId, userId, issuedCount);
            return true;

        } catch (Exception e) {
            log.error("쿠폰 발급 요청 Kafka 전송 실패: couponId={}, userId={}", couponId, userId, e);
            throw new CustomException(ErrorCode.ISSUE_COUPON_FAIL);
        }
    }

    /**
     * 중복 발급 확인 (1차 필터링 - 조회만)
     * Redis Set을 조회만 하고 추가하지 않습니다.
     * 실제 Set 추가는 Consumer에서 MySQL 성공 후 수행됩니다.
     */
    private boolean isAlreadyIssued(Long couponId, Long userId) {
        String issuedUsersKey = COUPON_ISSUED_USERS_PREFIX + couponId;
        Boolean isIssued = redisTemplate.opsForSet().isMember(issuedUsersKey, userId.toString());
        return isIssued != null && isIssued;
    }

    /**
     * 현재 발급된 쿠폰 개수 조회 (2차 필터링)
     * Redis SET의 크기(SCARD)를 사용하여 현재까지 발급된 쿠폰 개수를 확인합니다.
     *
     * @param couponId 쿠폰 ID
     * @return 발급된 쿠폰 개수
     */
    private Long getIssuedCount(Long couponId) {
        String issuedUsersKey = COUPON_ISSUED_USERS_PREFIX + couponId;
        Long count = redisTemplate.opsForSet().size(issuedUsersKey);
        log.debug("현재 발급된 쿠폰 개수: couponId={}, count={}", couponId, count);
        return count != null ? count : 0L;
    }
}
