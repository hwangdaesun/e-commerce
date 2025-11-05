package com.side.hhplusecommerce.order.repository;

import com.side.hhplusecommerce.order.domain.Order;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryOrderRepository implements OrderRepository {
    private final Map<Long, Order> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Optional<Order> findById(Long orderId) {
        return Optional.ofNullable(store.get(orderId));
    }

    @Override
    public Order save(Order order) {
        if (Objects.isNull(order.getOrderId())) {
            Long id = idGenerator.getAndIncrement();
            Order newOrder = Order.create(id, order.getUserId(), order.getTotalAmount(), order.getCouponDiscount());
            store.put(id, newOrder);
            return newOrder;
        }
        store.put(order.getOrderId(), order);
        return order;
    }
}