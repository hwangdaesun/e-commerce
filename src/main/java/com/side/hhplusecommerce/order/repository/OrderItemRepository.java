package com.side.hhplusecommerce.order.repository;

import com.side.hhplusecommerce.order.domain.OrderItem;

import java.util.List;
import java.util.Optional;

public interface OrderItemRepository {
    Optional<OrderItem> findById(Long orderItemId);
    OrderItem save(OrderItem orderItem);
    List<OrderItem> saveAll(List<OrderItem> orderItems);
}
