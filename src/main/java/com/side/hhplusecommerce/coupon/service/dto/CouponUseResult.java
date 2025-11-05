package com.side.hhplusecommerce.coupon.service.dto;

import com.side.hhplusecommerce.coupon.domain.Coupon;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CouponUseResult {
    private final Coupon coupon;
    private final Integer discountAmount;
}