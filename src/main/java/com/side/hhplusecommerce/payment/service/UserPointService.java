package com.side.hhplusecommerce.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserPointService {
    private final UserPointLockService userPointLockService;

    public void use(Long userId, Integer amount) {
        userPointLockService.usePointWithPessimisticLock(userId, amount);
    }
}
