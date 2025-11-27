package com.side.hhplusecommerce.item.usecase;

import com.side.hhplusecommerce.common.dto.CursorRequest;
import com.side.hhplusecommerce.item.controller.dto.ItemResponse;
import com.side.hhplusecommerce.item.controller.dto.ItemStockResponse;
import com.side.hhplusecommerce.item.controller.dto.ItemsResponse;
import com.side.hhplusecommerce.item.controller.dto.ItemsResponse.ItemInfo;
import com.side.hhplusecommerce.item.controller.dto.PopularItemsResponse;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.domain.ItemValidator;
import com.side.hhplusecommerce.item.dto.ItemDto;
import com.side.hhplusecommerce.item.dto.PopularItemsDto;
import com.side.hhplusecommerce.item.repository.ItemRepository;
import com.side.hhplusecommerce.item.service.ItemPopularityService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemViewUseCase {
    private final ItemRepository itemRepository;
    private final ItemPopularityService itemPopularityService;
    private final ItemValidator itemValidator;

    public ItemResponse view(Long itemId, boolean isPopular) {
        Item item;

        // 인기 상품이면 캐싱된 조회 메서드 사용
        if (isPopular) {
            ItemDto itemDto = itemPopularityService.getItemV1(itemId);
            return ItemResponse.from(itemDto);
        } else {
            item = itemValidator.validateExistence(itemId);
        }

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
                .map(ItemInfo::from)
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
        PopularItemsDto popularItemsDto = itemPopularityService.getPopularItemsV1(limit);
        return PopularItemsResponse.of(popularItemsDto.getItems());
    }
}
