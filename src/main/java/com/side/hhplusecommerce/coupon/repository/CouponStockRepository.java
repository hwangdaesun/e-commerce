package com.side.hhplusecommerce.coupon.repository;

import com.side.hhplusecommerce.coupon.domain.CouponStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponStockRepository extends JpaRepository<CouponStock, Long> {
    Optional<CouponStock> findByCouponId(Long couponId);
    CouponStock save(CouponStock couponStock);
    void deleteAll();
}