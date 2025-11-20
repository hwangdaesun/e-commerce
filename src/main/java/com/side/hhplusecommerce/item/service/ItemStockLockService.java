package com.side.hhplusecommerce.item.service;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemStockLockService {
    private final ItemRepository itemRepository;

    public void decreaseStockWithPessimisticLock(long itemId, int quantity) {
        Item item = itemRepository.findByIdWithPessimisticLock(itemId)
                .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));

        item.decrease(quantity);
        itemRepository.save(item);
    }

    public void increaseStockWithWithPessimisticLock(long itemId, int quantity) {
        Item item = itemRepository.findByIdWithPessimisticLock(itemId)
                .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));

        item.increase(quantity);
        itemRepository.save(item);
    }
}
