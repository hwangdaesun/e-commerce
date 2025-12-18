package com.side.hhplusecommerce.order.infrastructure.redis;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 주문 이벤트 추적 서비스
 * 재고 예약(StockReservedEvent)과 쿠폰 사용(CouponUsedEvent) 이벤트의 완료 여부를 Redis에서 추적합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventTracker {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String KEY_PREFIX = "order:event:tracker:";
    private static final String FIELD_STOCK_RESERVED = "stockReserved";
    private static final String FIELD_COUPON_USED = "couponUsed";
    private static final long TTL_HOURS = 24; // 24시간 후 자동 삭제

    /**
     * 주문 생성 시 이벤트 추적 초기화
     * @param orderId 주문 ID
     * @param hasCoupon 쿠폰 사용 여부
     */
    public void initialize(Long orderId, boolean hasCoupon) {
        String key = getKey(orderId);

        // 재고와 쿠폰 모두 false로 초기화
        redisTemplate.opsForHash().put(key, FIELD_STOCK_RESERVED, "false");
        redisTemplate.opsForHash().put(key, FIELD_COUPON_USED, hasCoupon ? "false" : "true"); // 쿠폰이 없으면 true

        // TTL 설정 (24시간)
        redisTemplate.expire(key, TTL_HOURS, TimeUnit.HOURS);

        log.info("OrderEventTracker 초기화: orderId={}, hasCoupon={}", orderId, hasCoupon);
    }

    /**
     * 재고 예약 완료 표시 및 결제 준비 여부 확인
     * @param orderId 주문 ID
     * @return 결제 준비 완료 여부 (재고 예약 && 쿠폰 사용 모두 완료)
     */
    public boolean markStockReserved(Long orderId) {
        String key = getKey(orderId);

        // 재고 예약 완료 표시
        redisTemplate.opsForHash().put(key, FIELD_STOCK_RESERVED, "true");

        log.info("StockReserved 표시: orderId={}", orderId);

        // 결제 준비 여부 확인
        return isReadyForPayment(orderId);
    }

    /**
     * 쿠폰 사용 완료 표시 및 결제 준비 여부 확인
     * @param orderId 주문 ID
     * @return 결제 준비 완료 여부 (재고 예약 && 쿠폰 사용 모두 완료)
     */
    public boolean markCouponUsed(Long orderId) {
        String key = getKey(orderId);

        // 쿠폰 사용 완료 표시
        redisTemplate.opsForHash().put(key, FIELD_COUPON_USED, "true");

        log.info("CouponUsed 표시: orderId={}", orderId);

        // 결제 준비 여부 확인
        return isReadyForPayment(orderId);
    }

    /**
     * 결제 준비 여부 확인 (재고 예약 && 쿠폰 사용 모두 완료)
     * @param orderId 주문 ID
     * @return 결제 준비 완료 여부
     */
    public boolean isReadyForPayment(Long orderId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(getKey(orderId));

        String stockReserved = (String) entries.get(FIELD_STOCK_RESERVED);
        String couponUsed = (String) entries.get(FIELD_COUPON_USED);

        return "true".equals(stockReserved) && "true".equals(couponUsed);
    }

    /**
     * 재고 예약 완료 여부 확인
     * @param orderId 주문 ID
     * @return 재고 예약 완료 여부
     */
    public boolean isStockReserved(Long orderId) {
        String key = getKey(orderId);
        String stockReserved = (String) redisTemplate.opsForHash().get(key, FIELD_STOCK_RESERVED);
        return "true".equals(stockReserved);
    }

    /**
     * 쿠폰 사용 완료 여부 확인
     * @param orderId 주문 ID
     * @return 쿠폰 사용 완료 여부
     */
    public boolean isCouponUsed(Long orderId) {
        String key = getKey(orderId);
        String couponUsed = (String) redisTemplate.opsForHash().get(key, FIELD_COUPON_USED);
        return "true".equals(couponUsed);
    }

    /**
     * 이벤트 추적 데이터 삭제
     * @param orderId 주문 ID
     */
    public void delete(Long orderId) {
        String key = getKey(orderId);
        redisTemplate.delete(key);
        log.info("OrderEventTracker 삭제: orderId={}", orderId);
    }

    private String getKey(Long orderId) {
        return KEY_PREFIX + orderId;
    }
}
