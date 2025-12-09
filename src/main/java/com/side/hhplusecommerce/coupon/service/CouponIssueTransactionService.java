package com.side.hhplusecommerce.coupon.service;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.coupon.controller.dto.IssueCouponResponse;
import com.side.hhplusecommerce.coupon.domain.Coupon;
import com.side.hhplusecommerce.coupon.domain.CouponIssueValidator;
import com.side.hhplusecommerce.coupon.domain.CouponStock;
import com.side.hhplusecommerce.coupon.domain.UserCoupon;
import com.side.hhplusecommerce.coupon.repository.CouponRepository;
import com.side.hhplusecommerce.coupon.repository.CouponStockRepository;
import com.side.hhplusecommerce.coupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 쿠폰 발급 트랜잭션 서비스 - 트랜잭션 레이어
 * 트랜잭션만 적용하고 락은 외부 서비스(CouponIssueLockService)에서 처리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponIssueTransactionService {
    private final CouponRepository couponRepository;
    private final CouponStockRepository couponStockRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponIssueValidator couponIssueValidator;

    /**
     * 쿠폰 발급 (트랜잭션 처리)
     * 분산락이 외부에서 적용되므로, 비관적 락 없이 일반 조회를 사용합니다.
     */
    @Transactional
    public IssueCouponResponse issueCoupon(Long couponId, Long userId) {
        // 쿠폰 재고 조회 (분산락으로 동시성 제어되므로 비관적 락 불필요)
        CouponStock couponStock = couponStockRepository.findByCouponId(couponId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_FOUND));

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_FOUND));

        // 쿠폰 만료 검증
        if (isExpired(coupon.getExpiresAt())) {
            throw new CustomException(ErrorCode.EXPIRED_COUPON);
        }

        // 중복 발급 검증
        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId);
        couponIssueValidator.validateNotAlreadyIssued(couponId, userCoupons);

        // 재고 차감
        couponStock.decrease();
        couponStockRepository.save(couponStock);

        // 사용자 쿠폰 발급
        UserCoupon userCoupon = UserCoupon.issue(userId, couponId);
        UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);

        return IssueCouponResponse.of(savedUserCoupon, coupon);
    }

    private boolean isExpired(LocalDateTime expiresAt) {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
