package com.side.hhplusecommerce.item.repository;

import com.side.hhplusecommerce.item.domain.ItemView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ItemViewRepository extends JpaRepository<ItemView, Long> {
    @Query("SELECT COUNT(iv) FROM ItemView iv WHERE iv.itemId = :itemId AND iv.createdAt > :after")
    Long countByItemIdAndCreatedAtAfter(@Param("itemId") Long itemId, @Param("after") LocalDateTime after);
}
