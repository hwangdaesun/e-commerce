package com.side.hhplusecommerce.coupon.repository;

import com.side.hhplusecommerce.coupon.domain.CouponStock;

import java.util.Optional;

public interface CouponStockRepository {
    Optional<CouponStock> findByCouponId(Long couponId);
    CouponStock save(CouponStock couponStock);
    void deleteAll();
}