package com.side.hhplusecommerce.coupon.service;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.common.lock.PessimisticLock;
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

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponIssueLockService {
    private final CouponRepository couponRepository;
    private final CouponStockRepository couponStockRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponIssueValidator couponIssueValidator;
    private final CouponRollbackHandler couponRollbackHandler;

    @PessimisticLock(timeout = 3)
    public IssueCouponResponse issueCouponWithPessimisticLock(Long couponId, Long userId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_FOUND));

        if (isExpired(coupon.getExpiresAt())) {
            throw new CustomException(ErrorCode.EXPIRED_COUPON);
        }

        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId);
        couponIssueValidator.validateNotAlreadyIssued(couponId, userCoupons);

        CouponStock couponStock = couponStockRepository.findByCouponId(couponId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_FOUND));

        couponStock.decrease();
        couponStockRepository.save(couponStock);

        try {
            UserCoupon userCoupon = UserCoupon.issue(userId, couponId);
            UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);
            return IssueCouponResponse.of(savedUserCoupon, coupon);

        } catch (Exception e) {
            couponRollbackHandler.rollbackCouponStock(couponStock, couponId, userId);
            throw e;
        }
    }

    private boolean isExpired(LocalDateTime expiresAt) {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
