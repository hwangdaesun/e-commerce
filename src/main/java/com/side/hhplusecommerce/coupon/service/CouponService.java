package com.side.hhplusecommerce.coupon.service;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.coupon.domain.Coupon;
import com.side.hhplusecommerce.coupon.domain.UserCoupon;
import com.side.hhplusecommerce.coupon.repository.CouponRepository;
import com.side.hhplusecommerce.coupon.repository.UserCouponRepository;
import com.side.hhplusecommerce.coupon.service.dto.CouponUseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponService {
    private final UserCouponRepository userCouponRepository;
    private final CouponRepository couponRepository;

    public CouponUseResult useCoupon(Long userCouponId) {
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_FOUND));

        Coupon coupon = couponRepository.findById(userCoupon.getCouponId())
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_FOUND));

        userCoupon.use(coupon.getExpiresAt());
        userCouponRepository.save(userCoupon);

        return new CouponUseResult(coupon, coupon.getDiscountAmount());
    }
}
