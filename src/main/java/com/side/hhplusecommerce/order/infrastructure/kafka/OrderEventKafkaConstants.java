package com.side.hhplusecommerce.order.infrastructure.kafka;

/**
 * 주문 이벤트 Kafka 관련 상수
 */
public class OrderEventKafkaConstants {

    private OrderEventKafkaConstants() {
        // 유틸리티 클래스
    }

    /**
     * Kafka Topic 이름들
     */
    public static final String TOPIC_ORDER_CREATED = "order-created";
    public static final String TOPIC_STOCK_RESERVED = "stock-reserved";
    public static final String TOPIC_STOCK_FAILED = "stock-failed";
    public static final String TOPIC_COUPON_USED = "coupon-used";
    public static final String TOPIC_COUPON_FAILED = "coupon-failed";
    public static final String TOPIC_PROCESS_PAYMENT = "process-payment";
    public static final String TOPIC_ORDER_COMPLETED = "order-completed";
    public static final String TOPIC_COMPENSATE_STOCK = "compensate-stock";
    public static final String TOPIC_COMPENSATE_COUPON = "compensate-coupon";
    public static final String TOPIC_SEND_ORDER_DATA = "send-order-data";

    /**
     * Consumer Group ID들
     */
    public static final String GROUP_STOCK_SERVICE = "stock-service-group";
    public static final String GROUP_COUPON_SERVICE = "coupon-service-group";
    public static final String GROUP_PAYMENT_SERVICE = "payment-service-group";
    public static final String GROUP_ORDER_FLOW_MANAGER = "order-flow-manager-group";
    public static final String GROUP_POST_PROCESS = "post-process-group";
    public static final String GROUP_EXTERNAL_DATA_SERVICE = "external-data-service-group";
}