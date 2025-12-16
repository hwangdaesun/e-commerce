package com.side.hhplusecommerce.coupon.infrastructure.kafka;

/**
 * 쿠폰 발급 Kafka 관련 상수
 */
public class CouponIssueKafkaConstants {

    private CouponIssueKafkaConstants() {
        // 유틸리티 클래스
    }

    /**
     * Kafka Topic 이름
     */
    public static final String TOPIC_COUPON_ISSUE = "issue_coupon";

    /**
     * Consumer Group ID
     */
    public static final String CONSUMER_GROUP = "coupon-issue-group";

    /**
     * Redis 키: 쿠폰 재고 (필터링용)
     * Key: coupon:stock:{couponId}
     * Value: Integer (남은 재고 수)
     */
    public static final String COUPON_STOCK_PREFIX = "coupon:stock:";

    /**
     * Redis 키: 쿠폰 발급 사용자 Set (중복 발급 방지용 및 발급 개수 확인용)
     * Key: coupon:issued:users:{couponId}
     * Value: Set<String> (userId 목록)
     *
     * Producer: SET 크기(SCARD)로 발급 개수 확인 및 중복 검사
     * Consumer: SET에 userId 추가
     */
    public static final String COUPON_ISSUED_USERS_PREFIX = "coupon:issued:users:";

    /**
     * 재고 배율 (실제 재고의 2배까지 큐에 추가)
     */
    public static final int STOCK_MULTIPLIER = 2;
}