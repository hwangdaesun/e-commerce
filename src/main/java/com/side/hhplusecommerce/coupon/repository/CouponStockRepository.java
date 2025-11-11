package com.side.hhplusecommerce.coupon.repository;

import com.side.hhplusecommerce.coupon.domain.CouponStock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponStockRepository extends JpaRepository<CouponStock, Long> {
    Optional<CouponStock> findByCouponId(Long couponId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cs FROM CouponStock cs WHERE cs.couponId = :couponId")
    Optional<CouponStock> findByCouponIdWithPessimisticLock(@Param("couponId") Long couponId);
}