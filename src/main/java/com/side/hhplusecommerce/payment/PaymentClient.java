package com.side.hhplusecommerce.payment;

public interface PaymentClient {
    PaymentResult pay(Long orderId, Long userId, Integer amount);
}