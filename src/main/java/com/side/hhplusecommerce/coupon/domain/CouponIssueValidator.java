package com.side.hhplusecommerce.coupon.domain;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CouponIssueValidator {

    public void validateNotAlreadyIssued(Long couponId, List<UserCoupon> userCoupons) {
        boolean alreadyIssued = userCoupons.stream()
                .anyMatch(userCoupon -> userCoupon.getCouponId().equals(couponId));

        if (alreadyIssued) {
            throw new CustomException(ErrorCode.ALREADY_ISSUED_COUPON);
        }
    }
}