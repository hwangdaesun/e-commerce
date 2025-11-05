package com.side.hhplusecommerce.coupon.repository;

import com.side.hhplusecommerce.coupon.domain.Coupon;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryCouponRepository implements CouponRepository {
    private final Map<Long, Coupon> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Optional<Coupon> findById(Long couponId) {
        return Optional.ofNullable(store.get(couponId));
    }

    @Override
    public Coupon save(Coupon coupon) {
        if (Objects.isNull(coupon.getCouponId())) {
            Long id = idGenerator.getAndIncrement();
            Coupon newCoupon = Coupon.builder()
                    .couponId(id)
                    .name(coupon.getName())
                    .discountAmount(coupon.getDiscountAmount())
                    .totalQuantity(coupon.getTotalQuantity())
                    .expiresAt(coupon.getExpiresAt())
                    .build();
            store.put(id, newCoupon);
            return newCoupon;
        }
        store.put(coupon.getCouponId(), coupon);
        return coupon;
    }
}