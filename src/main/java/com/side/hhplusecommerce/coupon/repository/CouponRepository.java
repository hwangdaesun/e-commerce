package com.side.hhplusecommerce.coupon.repository;

import com.side.hhplusecommerce.coupon.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
}