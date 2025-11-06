package com.side.hhplusecommerce.coupon.repository;

import com.side.hhplusecommerce.coupon.domain.UserCoupon;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryUserCouponRepository implements UserCouponRepository {
    private final Map<Long, UserCoupon> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public List<UserCoupon> findByUserId(Long userId) {
        return store.values().stream()
                .filter(userCoupon -> userCoupon.getUserId().equals(userId))
                .toList();
    }

    @Override
    public Optional<UserCoupon> findById(Long userCouponId) {
        return Optional.ofNullable(store.get(userCouponId));
    }

    @Override
    public Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId) {
        return store.values().stream()
                .filter(userCoupon -> userCoupon.getUserId().equals(userId)
                        && userCoupon.getCouponId().equals(couponId))
                .findFirst();
    }

    @Override
    public UserCoupon save(UserCoupon userCoupon) {
        if (Objects.isNull(userCoupon.getUserCouponId())) {
            Long id = idGenerator.getAndIncrement();
            UserCoupon newUserCoupon = UserCoupon.createWithId(
                    id,
                    userCoupon.getUserId(),
                    userCoupon.getCouponId(),
                    userCoupon.getIsUsed(),
                    userCoupon.getUsedAt(),
                    userCoupon.getIssuedAt()
            );
            store.put(id, newUserCoupon);
            return newUserCoupon;
        }
        store.put(userCoupon.getUserCouponId(), userCoupon);
        return userCoupon;
    }

    @Override
    public void delete(UserCoupon userCoupon) {
        store.remove(userCoupon.getUserCouponId());
    }
}