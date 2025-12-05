package com.side.hhplusecommerce.coupon.infrastructure.redis;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.coupon.infrastructure.redis.dto.CouponIssueMessage;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import static com.side.hhplusecommerce.coupon.infrastructure.redis.CouponIssueQueueConstants.*;

/**
 * 쿠폰 발급 큐 Producer
 * Redis Stream에 쿠폰 발급 요청을 추가합니다.
 *
 * 역할:
 * 1. Redis 재고 확인 (1차 필터링 - 조회만)
 * 2. Redis Set 중복 발급 확인 (2차 필터링 - 조회만)
 * 3. Redis Stream에 메시지 추가
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueQueueProducer {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 쿠폰 발급 요청을 큐에 추가
     *
     * @param couponId 쿠폰 ID
     * @param userId 사용자 ID
     * @return 성공 여부
     */
    public boolean enqueue(Long couponId, Long userId) {
        // 1. Redis 재고 확인 (1차 필터링 - 조회만)
        if (!checkRedisStock(couponId)) {
            log.warn("쿠폰 재고 부족 (Redis 필터링): couponId={}", couponId);
            throw new CustomException(ErrorCode.COUPON_SOLD_OUT);
        }

        // 2. 중복 발급 확인 (2차 필터링 - 조회만)
        // 동시성 이슈로 중복 요청이 큐에 들어갈 수 있지만, Consumer에서 MySQL로 최종 검증
        if (isAlreadyIssued(couponId, userId)) {
            log.warn("이미 발급된 쿠폰 (Redis 필터링): couponId={}, userId={}", couponId, userId);
            throw new CustomException(ErrorCode.ALREADY_ISSUED_COUPON);
        }

        // 3. Redis Stream에 메시지 추가
        try {
            CouponIssueMessage message = CouponIssueMessage.of(userId, couponId);
            Map<String, Object> messageMap = Map.of(
                    FIELD_USER_ID, userId.toString(),
                    FIELD_COUPON_ID, couponId.toString(),
                    FIELD_REQUEST_TIME, message.getRequestTime().toString()
            );

            String messageId = redisTemplate.opsForStream().add(COUPON_ISSUE_QUEUE, messageMap).getValue();
            log.info("쿠폰 발급 요청이 큐에 추가됨: messageId={}, couponId={}, userId={}",
                    messageId, couponId, userId);

            return true;
        } catch (Exception e) {
            log.error("쿠폰 발급 요청 큐 추가 실패: couponId={}, userId={}", couponId, userId, e);
            // 큐 추가 실패 시 일반적인 주문 실패로 처리
            throw new CustomException(ErrorCode.ISSUE_COUPON_FAIL);
        }
    }

    /**
     * Redis 재고 확인 (1차 필터링 - 조회만)
     * 정합성이 맞지 않아도 됨 - 필터링 역할
     */
    private boolean checkRedisStock(Long couponId) {
        String stockKey = COUPON_STOCK_PREFIX + couponId;
        Integer stock = (Integer) redisTemplate.opsForValue().get(stockKey);

        if (Objects.isNull(stock)) {
            log.warn("Redis 재고 정보 없음: couponId={}", couponId);
            return false;
        }

        return stock > 0;
    }

    /**
     * 중복 발급 확인 (2차 필터링 - 조회만)
     * Redis Set을 조회만 하고 추가하지 않습니다.
     * 실제 Set 추가는 Consumer에서 MySQL 성공 후 수행됩니다.
     */
    private boolean isAlreadyIssued(Long couponId, Long userId) {
        String issuedUsersKey = COUPON_ISSUED_USERS_PREFIX + couponId;
        Boolean isIssued = redisTemplate.opsForSet().isMember(issuedUsersKey, userId.toString());

        // 이미 발급된 경우 true 반환
        return isIssued != null && isIssued;
    }
}
