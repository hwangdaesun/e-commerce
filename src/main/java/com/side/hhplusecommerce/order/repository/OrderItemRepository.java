package com.side.hhplusecommerce.order.repository;

import com.side.hhplusecommerce.order.domain.OrderItem;
import com.side.hhplusecommerce.order.dto.ItemSalesCountDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @Query("SELECT new com.side.hhplusecommerce.order.dto.ItemSalesCountDto(oi.itemId, SUM(oi.quantity)) " +
           "FROM OrderItem oi " +
           "JOIN Order o ON oi.orderId = o.orderId " +
           "WHERE o.status = 'PAID' AND o.createdAt > :after " +
           "GROUP BY oi.itemId")
    List<ItemSalesCountDto> countSalesByItemIdGrouped(@Param("after") LocalDateTime after);
}
