package com.side.hhplusecommerce.order.service;

import com.side.hhplusecommerce.order.domain.Order;
import com.side.hhplusecommerce.order.repository.OrderRepository;
import com.side.hhplusecommerce.payment.service.UserPointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderPaymentService {
    private final UserPointService userPointService;
    private final OrderRepository orderRepository;

    public void processOrderPayment(Long userId, Order order) {
        userPointService.usePointWithPessimisticLock(userId, order.getFinalAmount());

        order.completePay();
        orderRepository.save(order);
    }
}
