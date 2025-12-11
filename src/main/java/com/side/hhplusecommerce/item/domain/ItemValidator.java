package com.side.hhplusecommerce.item.domain;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemValidator {
    private final ItemRepository itemRepository;

    public Item validateExistence(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));
    }

    public List<Item> validateExistence(List<Long> itemIds) {
        List<Item> items = itemRepository.findAllByItemIdIn(itemIds);

        if (items.size() != itemIds.size()) {
            throw new CustomException(ErrorCode.ITEM_NOT_FOUND);
        }

        return items;
    }
}
