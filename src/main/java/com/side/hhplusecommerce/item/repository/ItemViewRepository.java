package com.side.hhplusecommerce.item.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ItemViewRepository {
    Map<Long, Long> countByItemIdsAndCreatedAtAfter(List<Long> itemIds, LocalDateTime after);
}
