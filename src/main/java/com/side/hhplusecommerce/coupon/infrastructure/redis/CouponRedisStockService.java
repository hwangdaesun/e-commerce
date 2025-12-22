package com.side.hhplusecommerce.coupon.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import static com.side.hhplusecommerce.coupon.infrastructure.kafka.CouponIssueKafkaConstants.*;

/**
 * Redis 기반 쿠폰 재고 관리 서비스
 *
 * 역할:
 * 1. MySQL의 쿠폰 재고를 Redis에 캐싱 (필터링용)
 * 2. Consumer에서 MySQL 저장 성공 후 Redis 재고 차감
 * 3. Redis Set에 발급된 사용자 추가 (중복 발급 방지용)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponRedisStockService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * MySQL의 쿠폰 재고를 Redis에 초기화
     * 실제 재고의 2배만큼 설정 (여유있게 큐에 받기 위함)
     *
     * @param couponId 쿠폰 ID
     * @param totalQuantity MySQL의 총 재고 수량
     */
    public void initializeStock(Long couponId, Integer totalQuantity) {
        String stockKey = COUPON_STOCK_PREFIX + couponId;
        Integer redisStock = totalQuantity * STOCK_MULTIPLIER;

        redisTemplate.opsForValue().set(stockKey, redisStock);
        log.info("Redis 쿠폰 재고 초기화: couponId={}, totalQuantity={}, redisStock={}",
                couponId, totalQuantity, redisStock);
    }

    /**
     * Redis 재고 조회
     *
     * @param couponId 쿠폰 ID
     * @return 남은 재고 수량
     */
    public Integer getStock(Long couponId) {
        String stockKey = COUPON_STOCK_PREFIX + couponId;
        Integer stock = (Integer) redisTemplate.opsForValue().get(stockKey);
        return stock != null ? stock : 0;
    }

    /**
     * Redis 재고 차감
     * Consumer에서 MySQL 저장 성공 후 호출됩니다.
     *
     * @param couponId 쿠폰 ID
     * @return 차감 후 재고 수량
     */
    public Long decreaseStock(Long couponId) {
        String stockKey = COUPON_STOCK_PREFIX + couponId;
        Long remainingStock = redisTemplate.opsForValue().decrement(stockKey);

        log.debug("Redis 쿠폰 재고 차감: couponId={}, remainingStock={}", couponId, remainingStock);
        return remainingStock != null ? remainingStock : 0L;
    }

    /**
     * Redis Set에 발급된 사용자 추가 (중복 발급 방지용)
     * Consumer에서 MySQL 저장 성공 후 호출됩니다.
     *
     * @param couponId 쿠폰 ID
     * @param userId 사용자 ID
     */
    public void addIssuedUser(Long couponId, Long userId) {
        String issuedUsersKey = COUPON_ISSUED_USERS_PREFIX + couponId;
        redisTemplate.opsForSet().add(issuedUsersKey, userId.toString());

        log.debug("Redis Set에 발급 사용자 추가: couponId={}, userId={}", couponId, userId);
    }

    /**
     * 발급된 사용자 확인
     *
     * @param couponId 쿠폰 ID
     * @param userId 사용자 ID
     * @return 이미 발급된 경우 true
     */
    public boolean isAlreadyIssued(Long couponId, Long userId) {
        String issuedUsersKey = COUPON_ISSUED_USERS_PREFIX + couponId;
        Boolean isMember = redisTemplate.opsForSet().isMember(issuedUsersKey, userId.toString());
        return isMember != null && isMember;
    }

    /**
     * Redis 재고 및 발급 사용자 정보 삭제
     * 쿠폰 종료 시 호출됩니다.
     *
     * @param couponId 쿠폰 ID
     */
    public void clearCouponData(Long couponId) {
        String stockKey = COUPON_STOCK_PREFIX + couponId;
        String issuedUsersKey = COUPON_ISSUED_USERS_PREFIX + couponId;

        redisTemplate.delete(stockKey);
        redisTemplate.delete(issuedUsersKey);

        log.info("Redis 쿠폰 데이터 삭제: couponId={}", couponId);
    }
}