package com.side.hhplusecommerce.coupon.repository;

import com.side.hhplusecommerce.coupon.domain.CouponStock;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryCouponStockRepository implements CouponStockRepository {
    private final Map<Long, CouponStock> store = new ConcurrentHashMap<>();

    @Override
    public Optional<CouponStock> findByCouponId(Long couponId) {
        return Optional.ofNullable(store.get(couponId));
    }

    @Override
    public CouponStock save(CouponStock couponStock) {
        store.put(couponStock.getCouponId(), couponStock);
        return couponStock;
    }
}