package com.side.hhplusecommerce.item.usecase;

import com.side.hhplusecommerce.common.dto.CursorRequest;
import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.item.controller.dto.ItemResponse;
import com.side.hhplusecommerce.item.controller.dto.ItemStockResponse;
import com.side.hhplusecommerce.item.controller.dto.ItemsResponse;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemViewUseCase {
    private final ItemRepository itemRepository;

    public ItemResponse view(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));

        return ItemResponse.from(item);
    }

    public ItemsResponse view(CursorRequest cursorRequest) {
        List<Item> items = itemRepository.findAllWithCursor(
                cursorRequest.getCursor(),
                cursorRequest.getSize()
        );

        boolean hasNext = items.size() > cursorRequest.getSize();
        List<ItemsResponse.ItemInfo> itemInfos = items.stream()
                .limit(cursorRequest.getSize())
                .map(ItemsResponse.ItemInfo::from)
                .collect(java.util.stream.Collectors.toList());

        Long nextCursor = null;
        if (hasNext && !itemInfos.isEmpty()) {
            nextCursor = itemInfos.get(itemInfos.size() - 1).getItemId();
        }

        return ItemsResponse.of(itemInfos, nextCursor, hasNext);
    }

    public ItemStockResponse viewStock(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));

        return ItemStockResponse.from(item);
    }
}
