package com.side.hhplusecommerce.payment.service;

import com.side.hhplusecommerce.common.lock.distributed.DistributedLock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserPointService {
    private final UserPointTransactionService userPointTransactionService;

    /**
     * 포인트 충전
     */
    @DistributedLock(keyResolver = "userPointLockKeyResolver", key = "#userId")
    public void charge(Long userId, Integer amount) {
        userPointTransactionService.chargePoint(userId, amount);
    }

    /**
     * 포인트 사용
     */
    @DistributedLock(keyResolver = "userPointLockKeyResolver", key = "#userId")
    public void use(Long userId, Integer amount) {
        userPointTransactionService.usePoint(userId, amount);
    }
}
