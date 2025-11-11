package com.side.hhplusecommerce.point.repository;

import com.side.hhplusecommerce.point.domain.UserPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPointRepository extends JpaRepository<UserPoint, Long> {
    Optional<UserPoint> findByUserId(Long userId);
    UserPoint save(UserPoint userPoint);
    void deleteAll();
}