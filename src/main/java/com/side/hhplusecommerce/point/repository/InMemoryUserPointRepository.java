package com.side.hhplusecommerce.point.repository;

import com.side.hhplusecommerce.point.domain.UserPoint;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryUserPointRepository implements UserPointRepository {
    private final Map<Long, UserPoint> store = new ConcurrentHashMap<>();

    @Override
    public Optional<UserPoint> findByUserId(Long userId) {
        return Optional.ofNullable(store.get(userId));
    }

    @Override
    public UserPoint save(UserPoint userPoint) {
        store.put(userPoint.getUserId(), userPoint);
        return userPoint;
    }
}