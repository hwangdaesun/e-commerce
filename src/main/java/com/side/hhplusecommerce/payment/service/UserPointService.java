package com.side.hhplusecommerce.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserPointService {
    private final UserPointLockService userPointLockService;

    @Transactional
    public void use(Long userId, Integer amount) {
        userPointLockService.usePointWithPessimisticLock(userId, amount);
    }
}
