package com.side.hhplusecommerce.order.service;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.order.domain.Order;
import com.side.hhplusecommerce.order.repository.OrderRepository;
import com.side.hhplusecommerce.payment.PaymentResult;
import com.side.hhplusecommerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderPaymentService {
    private final PaymentService paymentService;
    private final OrderRepository orderRepository;

    public void processOrderPayment(Long userId, Order order) {
        PaymentResult paymentResult = paymentService.processPayment(
                userId, order.getOrderId(), order.getFinalAmount());

        if (!paymentResult.isSuccess()) {
            throw new CustomException(ErrorCode.INSUFFICIENT_POINT);
        }

        order.completePay();
        orderRepository.save(order);
    }
}
