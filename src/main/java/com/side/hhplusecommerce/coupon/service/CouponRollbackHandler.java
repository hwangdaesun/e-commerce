package com.side.hhplusecommerce.coupon.service;

import com.side.hhplusecommerce.coupon.domain.CouponStock;
import com.side.hhplusecommerce.coupon.repository.CouponRepository;
import com.side.hhplusecommerce.coupon.repository.CouponStockRepository;
import com.side.hhplusecommerce.coupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponRollbackHandler {
    private final CouponRepository couponRepository;
    private final CouponStockRepository couponStockRepository;
    private final UserCouponRepository userCouponRepository;

    public void rollbackCouponStock(CouponStock couponStock, Long couponId, Long userId) {
        try {
            couponStock.increase();
            couponStockRepository.save(couponStock);

            userCouponRepository.findByUserIdAndCouponId(userId, couponId)
                    .ifPresent(userCouponRepository::delete);

        } catch (Exception rollbackException) {
            log.error("Failed to rollback coupon issue: couponId={}, userId={}", couponId, userId, rollbackException);
        }
    }

}
