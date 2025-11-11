package com.side.hhplusecommerce.item.repository;

import com.side.hhplusecommerce.item.domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ItemRepository {
    Optional<Item> findById(Long itemId);
    List<Item> findAllWithCursor(Long cursor, Integer size);
    List<Item> findPopularItems(Integer limit, LocalDateTime after);
    List<Item> findAllByIds(List<Long> itemIds);
    void save(Item item);
    void deleteAll();
public interface ItemRepository extends JpaRepository<Item, Long> {
}
