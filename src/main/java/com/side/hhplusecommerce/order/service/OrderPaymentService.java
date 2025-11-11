package com.side.hhplusecommerce.order.service;

import com.side.hhplusecommerce.order.domain.Order;
import com.side.hhplusecommerce.order.repository.OrderRepository;
import com.side.hhplusecommerce.payment.service.UserPointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderPaymentService {
    private final UserPointService userPointService;
    private final OrderRepository orderRepository;

    @Transactional
    public void processOrderPayment(Long userId, Order order) {
        userPointService.use(userId, order.getFinalAmount());

        order.completePay();
        orderRepository.save(order);
    }
}
