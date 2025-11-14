package com.side.hhplusecommerce.item.service;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemStockLockService {
    private final ItemRepository itemRepository;

    @Transactional
    public void decreaseStockWithOptimisticLock(long itemId, int quantity) {
        Item item = itemRepository.findByIdWithOptimisticLock(itemId)
                .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));

        item.decrease(quantity);
        itemRepository.save(item);
    }

    @Transactional
    public void increaseStockWithOptimisticLock(long itemId, int quantity) {
        Item item = itemRepository.findByIdWithOptimisticLock(itemId)
                .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));

        item.increase(quantity);
        itemRepository.save(item);
    }
}
