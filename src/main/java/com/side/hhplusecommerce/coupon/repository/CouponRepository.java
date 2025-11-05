package com.side.hhplusecommerce.coupon.repository;

import com.side.hhplusecommerce.coupon.domain.Coupon;

import java.util.Optional;

public interface CouponRepository {
    Optional<Coupon> findById(Long couponId);
    Coupon save(Coupon coupon);
}