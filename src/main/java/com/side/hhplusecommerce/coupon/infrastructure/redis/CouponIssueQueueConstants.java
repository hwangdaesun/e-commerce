package com.side.hhplusecommerce.coupon.infrastructure.redis;

/**
 * 쿠폰 발급 큐 관련 상수
 */
public class CouponIssueQueueConstants {

    private CouponIssueQueueConstants() {
        // 유틸리티 클래스
    }

    /**
     * Redis Stream 키 (쿠폰 발급 큐)
     */
    public static final String COUPON_ISSUE_QUEUE = "coupon:issue:queue";

    /**
     * Consumer Group 이름
     */
    public static final String CONSUMER_GROUP = "coupon-issue-group";

    /**
     * Consumer 이름 (인스턴스 ID와 조합하여 사용)
     */
    public static final String CONSUMER_PREFIX = "coupon-consumer-";

    /**
     * Redis 키: 쿠폰 재고 (필터링용)
     * Key: coupon:stock:{couponId}
     * Value: Integer (남은 재고 수)
     */
    public static final String COUPON_STOCK_PREFIX = "coupon:stock:";

    /**
     * Redis 키: 쿠폰 발급 사용자 Set (중복 발급 방지용)
     * Key: coupon:issued:users:{couponId}
     * Value: Set<String> (userId 목록)
     */
    public static final String COUPON_ISSUED_USERS_PREFIX = "coupon:issued:users:";

    /**
     * Stream 필드명
     */
    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_COUPON_ID = "couponId";
    public static final String FIELD_REQUEST_TIME = "requestTime";

    /**
     * 재고 배율 (실제 재고의 2배까지 큐에 추가)
     */
    public static final int STOCK_MULTIPLIER = 2;

    /**
     * 배치 처리 크기 (한 번에 처리할 메시지 수)
     */
    public static final int BATCH_SIZE = 100;

    /**
     * Consumer 타임아웃 (밀리초)
     */
    public static final long CONSUMER_TIMEOUT_MS = 5000L;
}