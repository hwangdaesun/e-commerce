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
public class UserPointLockService {
    private final UserPointRepository userPointRepository;

    @Transactional
    public void usePointWithPessimisticLock(Long userId, Integer amount) {
        UserPoint userPoint = userPointRepository.findByUserIdWithPessimisticLock(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_POINT_NOT_FOUND));

        userPoint.use(amount);
        userPointRepository.save(userPoint);
    }
}
