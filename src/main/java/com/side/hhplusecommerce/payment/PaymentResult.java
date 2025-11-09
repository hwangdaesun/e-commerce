package com.side.hhplusecommerce.payment;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentResult {
    private boolean success;
    private String message;

    public static PaymentResult success() {
        return new PaymentResult(true, "결제 성공");
    }

    public static PaymentResult failure(String message) {
        return new PaymentResult(false, message);
    }
}