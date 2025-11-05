package com.side.hhplusecommerce.payment.service;

import com.side.hhplusecommerce.payment.PaymentClient;
import com.side.hhplusecommerce.payment.PaymentResult;
import com.side.hhplusecommerce.point.domain.UserPoint;
import com.side.hhplusecommerce.point.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final UserPointRepository userPointRepository;
    private final PaymentClient paymentClient;

    public PaymentResult processPayment(Long userId, Long orderId, Integer amount) {
        UserPoint userPoint = userPointRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserPoint newUserPoint = UserPoint.initialize(userId);
                    return userPointRepository.save(newUserPoint);
                });

        userPoint.use(amount);
        userPointRepository.save(userPoint);

        return paymentClient.pay(orderId, userId, amount);
    }
}