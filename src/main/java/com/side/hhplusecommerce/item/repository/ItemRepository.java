package com.side.hhplusecommerce.item.repository;

import com.side.hhplusecommerce.item.domain.Item;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {
    @Query("SELECT i FROM Item i WHERE i.itemId > :cursor ORDER BY i.itemId ASC")
    List<Item> findAllWithCursor(@Param("cursor") Long cursor, Pageable pageable);

    @Query("SELECT i FROM Item i WHERE i.updatedAt > :after ORDER BY i.salesCount DESC")
    List<Item> findPopularItems(@Param("after") LocalDateTime after, Pageable pageable);

    @Query("SELECT i FROM Item i WHERE i.itemId IN :itemIds")
    List<Item> findAllByItemIdIn(@Param("itemIds") List<Long> itemIds);

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT i FROM Item i WHERE i.itemId = :itemId")
    Optional<Item> findByIdWithOptimisticLock(@Param("itemId") Long itemId);
}
