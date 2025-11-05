package com.side.hhplusecommerce.item.repository;

import com.side.hhplusecommerce.item.domain.Item;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ItemRepository {
    Optional<Item> findById(Long itemId);
    List<Item> findAllWithCursor(Long cursor, Integer size);
    List<Item> findPopularItems(Integer limit, LocalDateTime after);
    List<Item> findAllByIds(List<Long> itemIds);
}