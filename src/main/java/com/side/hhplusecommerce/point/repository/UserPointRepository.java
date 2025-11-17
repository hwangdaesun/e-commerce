package com.side.hhplusecommerce.point.repository;

import com.side.hhplusecommerce.point.domain.UserPoint;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserPointRepository extends JpaRepository<UserPoint, Long> {
    Optional<UserPoint> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT up FROM UserPoint up WHERE up.userId = :userId")
    Optional<UserPoint> findByUserIdWithPessimisticLock(@Param("userId") Long userId);
}
