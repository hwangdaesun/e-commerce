package com.side.hhplusecommerce.cart.usecase;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.coupon.domain.Coupon;
import com.side.hhplusecommerce.coupon.domain.CouponStock;
import com.side.hhplusecommerce.coupon.domain.UserCoupon;
import com.side.hhplusecommerce.coupon.repository.CouponRepository;
import com.side.hhplusecommerce.coupon.repository.CouponStockRepository;
import com.side.hhplusecommerce.coupon.repository.UserCouponRepository;
import com.side.hhplusecommerce.coupon.controller.dto.IssueCouponResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponIssueUseCase {
    private final CouponRepository couponRepository;
    private final CouponStockRepository couponStockRepository;
    private final UserCouponRepository userCouponRepository;

    public IssueCouponResponse issue(Long couponId, Long userId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_FOUND));

        if (isExpired(coupon.getExpiresAt())) {
            throw new CustomException(ErrorCode.EXPIRED_COUPON);
        }

        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId);
        boolean alreadyIssued = userCoupons.stream()
                .anyMatch(userCoupon -> userCoupon.getCouponId().equals(couponId));

        if (alreadyIssued) {
            throw new CustomException(ErrorCode.ALREADY_ISSUED_COUPON);
        }

        CouponStock couponStock = couponStockRepository.findByCouponId(couponId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_FOUND));

        couponStock.decrease();
        couponStockRepository.save(couponStock);

        UserCoupon userCoupon = UserCoupon.issue(userId, couponId);
        UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);

        return IssueCouponResponse.of(savedUserCoupon, coupon);
    }

    private boolean isExpired(LocalDateTime expiresAt) {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
