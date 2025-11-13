package com.side.hhplusecommerce.item.repository;

import com.side.hhplusecommerce.item.domain.ItemView;
import com.side.hhplusecommerce.item.dto.ItemViewCountDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ItemViewRepository extends JpaRepository<ItemView, Long> {

    @Query("SELECT new com.side.hhplusecommerce.item.dto.ItemViewCountDto(iv.itemId, COUNT(iv)) " +
           "FROM ItemView iv " +
           "WHERE iv.createdAt > :after " +
           "GROUP BY iv.itemId")
    List<ItemViewCountDto> countViewsByItemIdGrouped(@Param("after") LocalDateTime after);
}
