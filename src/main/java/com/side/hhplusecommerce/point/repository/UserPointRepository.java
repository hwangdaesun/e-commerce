package com.side.hhplusecommerce.point.repository;

import com.side.hhplusecommerce.point.domain.UserPoint;

import java.util.Optional;

public interface UserPointRepository {
    Optional<UserPoint> findByUserId(Long userId);
    UserPoint save(UserPoint userPoint);
}