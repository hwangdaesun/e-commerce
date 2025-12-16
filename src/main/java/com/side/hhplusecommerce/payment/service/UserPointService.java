package com.side.hhplusecommerce.payment.service;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.common.lock.distributed.DistributedLock;
import com.side.hhplusecommerce.point.domain.UserPoint;
import com.side.hhplusecommerce.point.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserPointService {

    private final UserPointRepository userPointRepository;

    /**
     * 포인트 충전
     */
    @Transactional
    @DistributedLock(keyResolver = "userPointLockKeyResolver", key = "#userId")
    public void charge(Long userId, Integer amount) {
        UserPoint userPoint = userPointRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_POINT_NOT_FOUND));

        userPoint.charge(amount);
        userPointRepository.save(userPoint);
    }

    /**
     * 포인트 사용
     */
    @Transactional
    @DistributedLock(keyResolver = "userPointLockKeyResolver", key = "#userId")
    public void use(Long userId, Integer amount) {
        UserPoint userPoint = userPointRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_POINT_NOT_FOUND));

        userPoint.use(amount);
        userPointRepository.save(userPoint);
    }
}
