package com.side.hhplusecommerce.item.service;

import com.side.hhplusecommerce.common.lock.OptimisticLock;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemStockLockService {
    private final ItemRepository itemRepository;

    @OptimisticLock(maxRetries = 5, retryDelay = 100)
    public void decreaseStockWithOptimisticLock(long itemId, int quantity, Item item) {
        item.decrease(quantity);
        item.increaseSalesCount(quantity);
        itemRepository.save(item);
    }

    @OptimisticLock(maxRetries = 5, retryDelay = 100)
    public void increaseStockWithOptimisticLock(long itemId, int quantity, Item item) {
        item.increase(quantity);
        item.decreaseSalesCount(quantity);
        itemRepository.save(item);
    }
}