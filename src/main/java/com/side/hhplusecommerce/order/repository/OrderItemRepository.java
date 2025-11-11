package com.side.hhplusecommerce.order.repository;

import com.side.hhplusecommerce.order.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
