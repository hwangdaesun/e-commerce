package com.side.hhplusecommerce.payment.service;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
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
                .orElseThrow(() -> new CustomException(ErrorCode.USER_POINT_NOT_FOUND));

        // 결제 처리
        PaymentResult paymentResult = paymentClient.pay(orderId, userId, amount);

        // 결제 성공 시에만 포인트 차감
        if (paymentResult.isSuccess()) {
            userPoint.use(amount);
            userPointRepository.save(userPoint);
        }

        return paymentResult;
    }
}