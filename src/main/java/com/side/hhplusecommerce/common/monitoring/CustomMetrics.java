package com.side.hhplusecommerce.common.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 커스텀 메트릭 유틸리티 클래스
 *
 * 비즈니스 도메인별 메트릭을 쉽게 수집하기 위한 헬퍼 클래스
 */
@Component
@RequiredArgsConstructor
public class CustomMetrics {

    private final MeterRegistry meterRegistry;

    // ============================================
    // 쿠폰 발급 관련 메트릭
    // ============================================

    /**
     * 쿠폰 발급 요청 카운터 증가
     */
    public void incrementCouponIssueRequest(String couponId) {
        Counter.builder("coupon.issue.requests")
                .description("쿠폰 발급 요청 수")
                .tag("coupon_id", couponId)
                .register(meterRegistry)
                .increment();
    }

    /**
     * 쿠폰 발급 성공 카운터 증가
     */
    public void incrementCouponIssueSuccess(String couponId) {
        Counter.builder("coupon.issue.success")
                .description("쿠폰 발급 성공 수")
                .tag("coupon_id", couponId)
                .register(meterRegistry)
                .increment();
    }

    /**
     * 쿠폰 발급 실패 카운터 증가
     */
    public void incrementCouponIssueFail(String couponId, String reason) {
        Counter.builder("coupon.issue.fail")
                .description("쿠폰 발급 실패 수")
                .tag("coupon_id", couponId)
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    /**
     * 쿠폰 발급 처리 시간 기록
     */
    public void recordCouponIssueTime(String couponId, long timeMs) {
        Timer.builder("coupon.issue.duration")
                .description("쿠폰 발급 처리 시간")
                .tag("coupon_id", couponId)
                .register(meterRegistry)
                .record(timeMs, TimeUnit.MILLISECONDS);
    }

    // ============================================
    // 주문 관련 메트릭
    // ============================================

    /**
     * 주문 생성 카운터 증가
     */
    public void incrementOrderCreate(String status) {
        Counter.builder("order.create")
                .description("주문 생성 수")
                .tag("status", status)
                .register(meterRegistry)
                .increment();
    }

    /**
     * 주문 결제 완료 카운터 증가
     */
    public void incrementOrderPaymentComplete() {
        Counter.builder("order.payment.complete")
                .description("주문 결제 완료 수")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 주문 처리 시간 기록
     */
    public void recordOrderProcessingTime(long timeMs) {
        Timer.builder("order.processing.duration")
                .description("주문 처리 시간")
                .register(meterRegistry)
                .record(timeMs, TimeUnit.MILLISECONDS);
    }

    // ============================================
    // 재고 관련 메트릭
    // ============================================

    /**
     * 재고 차감 카운터 증가
     */
    public void incrementStockDecrease(String productId, int quantity) {
        Counter.builder("stock.decrease")
                .description("재고 차감 수")
                .tag("product_id", productId)
                .register(meterRegistry)
                .increment(quantity);
    }

    /**
     * 재고 부족 카운터 증가
     */
    public void incrementStockShortage(String productId) {
        Counter.builder("stock.shortage")
                .description("재고 부족 발생 수")
                .tag("product_id", productId)
                .register(meterRegistry)
                .increment();
    }

    // ============================================
    // Kafka Consumer 관련 메트릭
    // ============================================

    /**
     * Kafka 메시지 처리 성공 카운터 증가
     */
    public void incrementKafkaMessageSuccess(String topic, String consumerGroup) {
        Counter.builder("kafka.message.success")
                .description("Kafka 메시지 처리 성공 수")
                .tag("topic", topic)
                .tag("consumer_group", consumerGroup)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Kafka 메시지 처리 실패 카운터 증가
     */
    public void incrementKafkaMessageFail(String topic, String consumerGroup, String errorType) {
        Counter.builder("kafka.message.fail")
                .description("Kafka 메시지 처리 실패 수")
                .tag("topic", topic)
                .tag("consumer_group", consumerGroup)
                .tag("error_type", errorType)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Kafka 메시지 처리 시간 기록
     */
    public void recordKafkaMessageProcessingTime(String topic, long timeMs) {
        Timer.builder("kafka.message.processing.duration")
                .description("Kafka 메시지 처리 시간")
                .tag("topic", topic)
                .register(meterRegistry)
                .record(timeMs, TimeUnit.MILLISECONDS);
    }
}