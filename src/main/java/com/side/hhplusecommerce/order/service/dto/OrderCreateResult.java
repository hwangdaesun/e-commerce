package com.side.hhplusecommerce.order.service.dto;

import com.side.hhplusecommerce.order.domain.Order;
import com.side.hhplusecommerce.order.domain.OrderItem;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class OrderCreateResult {
    private final Order order;
    private final List<OrderItem> orderItems;
}