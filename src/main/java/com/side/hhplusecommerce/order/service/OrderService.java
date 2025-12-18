package com.side.hhplusecommerce.order.service;

import com.side.hhplusecommerce.cart.domain.CartItem;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.order.domain.Order;
import com.side.hhplusecommerce.order.domain.OrderItem;
import com.side.hhplusecommerce.order.repository.OrderItemRepository;
import com.side.hhplusecommerce.order.repository.OrderRepository;
import com.side.hhplusecommerce.order.service.dto.OrderCreateResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public OrderCreateResult createOrder(Long userId, List<CartItem> cartItems, List<Item> items,
                                        Integer totalAmount, Integer couponDiscount, Long userCouponId) {
        Order order = Order.create(null, userId, totalAmount, couponDiscount);
        Order savedOrder = orderRepository.save(order);

        List<OrderItem> orderItems = OrderItem.createAll(
                savedOrder.getOrderId(),
                cartItems,
                items,
                userCouponId
        );

        List<OrderItem> savedOrderItems = orderItemRepository.saveAll(orderItems);

        // 팩터리 메서드를 사용하여 DTO 생성
        return OrderCreateResult.from(savedOrder, savedOrderItems);
    }

    @Transactional
    public void completeOrderPayment(Order order) {
        order.completePay();
    }

    @Transactional
    public void completeOrderPayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다."));
        order.completePay();
    }

    @Transactional
    public void failOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다."));
        order.fail();
    }

    @Transactional
    public void failOrder(Long orderId, String failReason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다."));
        order.fail(failReason);
    }

    @Transactional(readOnly = true)
    public Order findById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다."));
    }
}
