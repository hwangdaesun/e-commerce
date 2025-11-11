package com.side.hhplusecommerce.item.repository;

import com.side.hhplusecommerce.item.domain.ItemView;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ItemViewRepository {
    Map<Long, Long> countByItemIdsAndCreatedAtAfter(List<Long> itemIds, LocalDateTime after);
public interface ItemViewRepository extends JpaRepository<ItemView, Long> {
}
