package com.side.hhplusecommerce.coupon.service;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.coupon.domain.Coupon;
import com.side.hhplusecommerce.coupon.domain.CouponStock;
import com.side.hhplusecommerce.coupon.domain.UserCoupon;
import com.side.hhplusecommerce.coupon.repository.CouponRepository;
import com.side.hhplusecommerce.coupon.repository.CouponStockRepository;
import com.side.hhplusecommerce.coupon.repository.UserCouponRepository;
import com.side.hhplusecommerce.coupon.service.dto.CouponUseResult;
import com.side.hhplusecommerce.order.event.CompensateCouponCommand;
import com.side.hhplusecommerce.order.event.CouponFailedEvent;
import com.side.hhplusecommerce.order.event.CouponUsedEvent;
import com.side.hhplusecommerce.order.event.OrderCreatedEvent;
import com.side.hhplusecommerce.order.infrastructure.kafka.OrderEventKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.side.hhplusecommerce.order.infrastructure.kafka.OrderEventKafkaConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {
    private final UserCouponRepository userCouponRepository;
    private final CouponRepository couponRepository;
    private final CouponStockRepository couponStockRepository;
    private final OrderEventKafkaProducer kafkaProducer;

    @Transactional
    public CouponUseResult useCoupon(Long userCouponId) {
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_FOUND));

        Coupon coupon = couponRepository.findById(userCoupon.getCouponId())
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_FOUND));

        userCoupon.use(coupon.getExpiresAt());
        userCouponRepository.save(userCoupon);

        return new CouponUseResult(coupon, coupon.getDiscountAmount());
    }

    @Transactional
    public void cancelCouponUse(Long userCouponId) {
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_FOUND));

        userCoupon.cancelUse();
        userCouponRepository.save(userCoupon);
    }

    /**
     * 쿠폰 할인 금액 계산 (검증만 수행, 실제 사용은 하지 않음)
     */
    public Integer calculateDiscount(Long userCouponId, Integer totalAmount) {
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_FOUND));

        Coupon coupon = couponRepository.findById(userCoupon.getCouponId())
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_FOUND));

        // 쿠폰 유효성 검증 (이미 사용했는지, 만료되었는지 확인)
        if (Boolean.TRUE.equals(userCoupon.getIsUsed())) {
            throw new CustomException(ErrorCode.ALREADY_USED_COUPON);
        }
        if (userCoupon.isExpired(coupon.getExpiresAt())) {
            throw new CustomException(ErrorCode.EXPIRED_COUPON);
        }

        return coupon.getDiscountAmount();
    }

    /**
     * UserCouponId로 Coupon 정보 조회
     */
    public Coupon getCouponByUserCouponId(Long userCouponId) {
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_FOUND));

        return couponRepository.findById(userCoupon.getCouponId())
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_FOUND));
    }

    /**
     * 쿠폰 발급 가능 여부 확인 및 남은 재고 반환
     * 쿠폰이 유효한지(만료되지 않았는지) 검증하고 CouponStock의 remainingQuantity를 반환합니다.
     *
     * @param couponId 쿠폰 ID
     * @return 남은 재고 수량 (CouponStock.remainingQuantity)
     */
    public Integer validateAndGetRemainingQuantity(Long couponId) {
        // 쿠폰 조회 및 만료 검증
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_FOUND));

        // 쿠폰 만료 검증
        if (isExpired(coupon.getExpiresAt())) {
            throw new CustomException(ErrorCode.EXPIRED_COUPON);
        }

        // 쿠폰 재고 조회
        CouponStock couponStock = couponStockRepository.findByCouponId(couponId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_FOUND));

        return couponStock.getRemainingQuantity();
    }

    /**
     * 쿠폰 만료 여부 확인
     */
    private boolean isExpired(LocalDateTime expiresAt) {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * OrderCreatedEvent 처리 - 쿠폰 사용 처리
     */
    @Transactional
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        if (event.getUserCouponId() == null) {
            return;
        }

        log.info("CouponService received OrderCreatedEvent: orderId={}, userCouponId={}",
                event.getOrderId(), event.getUserCouponId());

        try {
            // 쿠폰 사용 처리 (직접 구현)
            UserCoupon userCoupon = userCouponRepository.findById(event.getUserCouponId())
                    .orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_FOUND));

            Coupon coupon = couponRepository.findById(userCoupon.getCouponId())
                    .orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_FOUND));

            userCoupon.use(coupon.getExpiresAt());

            log.info("Coupon used successfully for orderId={}", event.getOrderId());
            CouponUsedEvent couponUsedEvent = CouponUsedEvent.of(event.getOrderId());
            kafkaProducer.publish(TOPIC_COUPON_USED, event.getOrderId().toString(), couponUsedEvent);

        } catch (Exception e) {
            log.error("Coupon usage failed for orderId={}", event.getOrderId(), e);
            CouponFailedEvent couponFailedEvent = CouponFailedEvent.of(event.getOrderId(), e.getMessage());
            kafkaProducer.publish(TOPIC_COUPON_FAILED, event.getOrderId().toString(), couponFailedEvent);
        }
    }

    /**
     * CompensateCouponCommand 처리 - 쿠폰 복구 처리
     */
    @Transactional
    public void handleCompensateCouponCommand(CompensateCouponCommand command) {
        log.info("CouponService received CompensateCouponCommand: orderId={}, userCouponId={}",
                command.getOrderId(), command.getUserCouponId());

        try {
            // 쿠폰 사용 취소 (직접 구현)
            UserCoupon userCoupon = userCouponRepository.findById(command.getUserCouponId())
                    .orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_FOUND));

            userCoupon.cancelUse();

            log.info("Coupon compensation completed for orderId={}", command.getOrderId());

        } catch (Exception e) {
            log.error("Coupon compensation failed for orderId={}", command.getOrderId(), e);
        }
    }
}
