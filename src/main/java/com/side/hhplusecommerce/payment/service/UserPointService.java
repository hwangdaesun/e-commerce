package com.side.hhplusecommerce.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserPointService {
    private final UserPointLockService userPointLockService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void use(Long userId, Integer amount) {
        userPointLockService.usePointWithPessimisticLock(userId, amount);
    }
}
