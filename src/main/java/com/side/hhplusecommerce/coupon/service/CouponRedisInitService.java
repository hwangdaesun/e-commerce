package com.side.hhplusecommerce.coupon.service;

import com.side.hhplusecommerce.coupon.domain.CouponStock;
import com.side.hhplusecommerce.coupon.infrastructure.redis.CouponRedisStockService;
import com.side.hhplusecommerce.coupon.repository.CouponStockRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 쿠폰 Redis 초기화 서비스
 * 애플리케이션 시작 시 MySQL의 쿠폰 재고를 Redis에 동기화합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponRedisInitService {

    private final CouponStockRepository couponStockRepository;
    private final CouponRedisStockService couponRedisStockService;

    /**
     * 애플리케이션 시작 시 쿠폰 재고를 Redis에 초기화
     */
    @PostConstruct
    public void initializeCouponStocks() {
        log.info("쿠폰 재고 Redis 초기화 시작");

        try {
            List<CouponStock> couponStocks = couponStockRepository.findAll();

            for (CouponStock couponStock : couponStocks) {
                if (couponStock.hasRemainingQuantity()) {
                    couponRedisStockService.initializeStock(
                            couponStock.getCouponId(),
                            couponStock.getRemainingQuantity()
                    );
                }
            }

            log.info("쿠폰 재고 Redis 초기화 완료: count={}", couponStocks.size());
        } catch (Exception e) {
            log.error("쿠폰 재고 Redis 초기화 실패", e);
        }
    }

    /**
     * 특정 쿠폰의 재고를 Redis에 초기화 (새 쿠폰 생성 시 사용)
     *
     * @param couponId 쿠폰 ID
     */
    public void initializeCouponStock(Long couponId) {
        CouponStock couponStock = couponStockRepository.findByCouponId(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰 재고를 찾을 수 없습니다: " + couponId));

        couponRedisStockService.initializeStock(
                couponStock.getCouponId(),
                couponStock.getRemainingQuantity()
        );

        log.info("쿠폰 재고 Redis 초기화: couponId={}, quantity={}",
                couponId, couponStock.getRemainingQuantity());
    }
}