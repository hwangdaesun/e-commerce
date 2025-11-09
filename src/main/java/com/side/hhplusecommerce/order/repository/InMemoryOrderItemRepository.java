package com.side.hhplusecommerce.order.repository;

import com.side.hhplusecommerce.order.domain.OrderItem;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryOrderItemRepository implements OrderItemRepository {
    private final Map<Long, OrderItem> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Optional<OrderItem> findById(Long orderItemId) {
        return Optional.ofNullable(store.get(orderItemId));
    }

    @Override
    public OrderItem save(OrderItem orderItem) {
        if (Objects.isNull(orderItem.getOrderItemId())) {
            Long id = idGenerator.getAndIncrement();
            OrderItem newOrderItem = OrderItem.createWithId(
                    id,
                    orderItem.getOrderId(),
                    orderItem.getItemId(),
                    orderItem.getName(),
                    orderItem.getPrice(),
                    orderItem.getQuantity(),
                    orderItem.getUserCouponId()
            );
            store.put(id, newOrderItem);
            return newOrderItem;
        }
        store.put(orderItem.getOrderItemId(), orderItem);
        return orderItem;
    }

    @Override
    public List<OrderItem> saveAll(List<OrderItem> orderItems) {
        return orderItems.stream()
                .map(orderItem -> {
                    if (Objects.isNull(orderItem.getOrderItemId())) {
                        Long id = idGenerator.getAndIncrement();
                        OrderItem newOrderItem = OrderItem.createWithId(
                                id,
                                orderItem.getOrderId(),
                                orderItem.getItemId(),
                                orderItem.getName(),
                                orderItem.getPrice(),
                                orderItem.getQuantity(),
                                orderItem.getUserCouponId()
                        );
                        store.put(id, newOrderItem);
                        return newOrderItem;
                    }
                    store.put(orderItem.getOrderItemId(), orderItem);
                    return orderItem;
                })
                .toList();
    }
}
