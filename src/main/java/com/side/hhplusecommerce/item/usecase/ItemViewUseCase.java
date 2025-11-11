package com.side.hhplusecommerce.item.usecase;

import com.side.hhplusecommerce.common.dto.CursorRequest;
import com.side.hhplusecommerce.item.controller.dto.ItemResponse;
import com.side.hhplusecommerce.item.controller.dto.ItemStockResponse;
import com.side.hhplusecommerce.item.controller.dto.ItemsResponse;
import com.side.hhplusecommerce.item.controller.dto.PopularItemsResponse;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.domain.ItemValidator;
import com.side.hhplusecommerce.item.repository.ItemRepository;
import com.side.hhplusecommerce.item.service.ItemPopularityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.side.hhplusecommerce.item.repository.ItemViewRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemViewUseCase {
    private final ItemRepository itemRepository;
    private final ItemViewRepository itemViewRepository;
    private final ItemPopularityService itemPopularityService;
    private final ItemValidator itemValidator;

    public ItemResponse view(Long itemId) {
        Item item = itemValidator.validateExistence(itemId);

        return ItemResponse.from(item);
    }

    public ItemsResponse view(CursorRequest cursorRequest) {
        Pageable pageable =
            PageRequest.of(0, cursorRequest.getSize() + 1);

        List<Item> items = itemRepository.findAllWithCursor(
                cursorRequest.getCursor(),
                pageable
        );

        boolean hasNext = items.size() > cursorRequest.getSize();
        List<ItemsResponse.ItemInfo> itemInfos = items.stream()
                .limit(cursorRequest.getSize())
                .map(ItemsResponse.ItemInfo::from)
                .toList();

        Long nextCursor = null;
        if (hasNext && !itemInfos.isEmpty()) {
            nextCursor = itemInfos.get(itemInfos.size() - 1).getItemId();
        }

        return ItemsResponse.of(itemInfos, nextCursor, hasNext);
    }

    public ItemStockResponse viewStock(Long itemId) {
        Item item = itemValidator.validateExistence(itemId);

        return ItemStockResponse.from(item);
    }

    public PopularItemsResponse viewPopular(Integer limit) {
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        Pageable pageable =
            PageRequest.of(0, limit);

        List<Item> items = itemRepository.findPopularItems(threeDaysAgo, pageable);
        List<Long> itemIds = items.stream()
                .map(Item::getItemId)
                .toList();

        Map<Long, Long> viewCount = new java.util.HashMap<>();
        for (Long itemId : itemIds) {
            Long count = itemViewRepository.countByItemIdAndCreatedAtAfter(itemId, threeDaysAgo);
            viewCount.put(itemId, count);
        }
        List<Long> popularItemIds = itemPopularityService.getPopularItemIds(items, viewCount, limit);

        List<Item> popularItems = itemRepository.findAllByItemIdIn(popularItemIds);
        return PopularItemsResponse.of(popularItems);
    }
}
