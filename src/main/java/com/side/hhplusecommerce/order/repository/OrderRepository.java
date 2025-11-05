package com.side.hhplusecommerce.order.repository;

import com.side.hhplusecommerce.order.domain.Order;

import java.util.Optional;

public interface OrderRepository {
    Optional<Order> findById(Long orderId);
    Order save(Order order);
}