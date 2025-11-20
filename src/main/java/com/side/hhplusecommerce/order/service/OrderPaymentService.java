package com.side.hhplusecommerce.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderPaymentService {
    // OrderPaymentService는 더 이상 사용되지 않음
    // 포인트 사용은 UserPointService.use()를 직접 호출
    // 주문 상태 변경은 OrderService.completeOrderPayment()를 사용
}
