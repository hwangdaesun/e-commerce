package com.side.hhplusecommerce.coupon.repository;

import com.side.hhplusecommerce.coupon.domain.UserCoupon;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository {
    List<UserCoupon> findByUserId(Long userId);
    Optional<UserCoupon> findById(Long userCouponId);
    Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId);
    UserCoupon save(UserCoupon userCoupon);
    void delete(UserCoupon userCoupon);
}