package com.side.hhplusecommerce.coupon.scheduler;

import com.side.hhplusecommerce.coupon.infrastructure.redis.CouponIssueQueueConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 쿠폰 발급 스케줄러
 * Redis Stream Consumer를 주기적으로 실행하여 큐에 쌓인 메시지를 처리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueScheduler {

    private final CouponIssueQueueConsumer couponIssueQueueConsumer;

    /**
     * 쿠폰 발급 큐 처리
     * 1초마다 실행
     */
    @Scheduled(fixedDelay = 1000)
    public void processCouponIssueQueue() {
        try {
            int processedCount = couponIssueQueueConsumer.processBatch();
            if (processedCount > 0) {
                log.info("쿠폰 발급 큐 처리 완료: processedCount={}", processedCount);
            }
        } catch (Exception e) {
            log.error("쿠폰 발급 큐 처리 중 오류 발생", e);
        }
    }

    /**
     * Pending 메시지 재처리
     * 10초마다 실행
     */
    @Scheduled(fixedDelay = 10000)
    public void processPendingMessages() {
        try {
            int processedCount = couponIssueQueueConsumer.processPendingMessages();
            if (processedCount > 0) {
                log.info("Pending 메시지 재처리 완료: processedCount={}", processedCount);
            }
        } catch (Exception e) {
            log.error("Pending 메시지 재처리 중 오류 발생", e);
        }
    }
}