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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        Map<Long, Item> itemMap = items.stream()
                .collect(Collectors.toMap(Item::getItemId, item -> item));

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            Item item = itemMap.get(cartItem.getItemId());
            if (item != null) {
                OrderItem orderItem = OrderItem.create(
                        savedOrder.getOrderId(),
                        item.getItemId(),
                        item.getName(),
                        item.getPrice(),
                        cartItem.getQuantity(),
                        userCouponId
                );
                orderItems.add(orderItem);
            }
        }

        List<OrderItem> savedOrderItems = orderItemRepository.saveAll(orderItems);

        return new OrderCreateResult(savedOrder, savedOrderItems);
    }
}
