package com.side.hhplusecommerce.payment.service;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.point.domain.UserPoint;
import com.side.hhplusecommerce.point.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserPointTransactionService {
    private final UserPointRepository userPointRepository;

    /**
     * 포인트 충전 (트랜잭션 처리)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void chargePoint(Long userId, Integer amount) {
        UserPoint userPoint = userPointRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_POINT_NOT_FOUND));

        userPoint.charge(amount);
        userPointRepository.save(userPoint);
    }

    /**
     * 포인트 사용 (트랜잭션 처리)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void usePoint(Long userId, Integer amount) {
        UserPoint userPoint = userPointRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_POINT_NOT_FOUND));

        userPoint.use(amount);
        userPointRepository.save(userPoint);
    }
}
