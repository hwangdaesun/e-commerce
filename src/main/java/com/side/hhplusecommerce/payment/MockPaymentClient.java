package com.side.hhplusecommerce.payment;

import org.springframework.stereotype.Component;

@Component
public class MockPaymentClient implements PaymentClient {

    @Override
    public PaymentResult pay(Long orderId, Long userId, Integer amount) {
        return PaymentResult.success();
    }
}