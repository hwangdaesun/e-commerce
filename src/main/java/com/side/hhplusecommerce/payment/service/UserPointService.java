package com.side.hhplusecommerce.payment.service;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.point.domain.UserPoint;
import com.side.hhplusecommerce.point.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserPointService {
    private final UserPointRepository userPointRepository;

    public void use(Long userId, Integer amount) {
        UserPoint userPoint = userPointRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_POINT_NOT_FOUND));

        userPoint.use(amount);
        userPointRepository.save(userPoint);
    }
}