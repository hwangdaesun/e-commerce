package com.side.hhplusecommerce.item.usecase;

import com.side.hhplusecommerce.common.dto.CursorRequest;
import com.side.hhplusecommerce.item.constants.PopularityPeriod;
import com.side.hhplusecommerce.item.controller.dto.ItemResponse;
import com.side.hhplusecommerce.item.controller.dto.ItemStockResponse;
import com.side.hhplusecommerce.item.controller.dto.ItemsResponse;
import com.side.hhplusecommerce.item.controller.dto.ItemsResponse.ItemInfo;
import com.side.hhplusecommerce.item.controller.dto.PopularItemsResponse;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.domain.ItemValidator;
import com.side.hhplusecommerce.item.dto.PopularItemsDto;
import com.side.hhplusecommerce.item.repository.ItemRepository;
import com.side.hhplusecommerce.item.service.ItemPopularityService;
import com.side.hhplusecommerce.item.service.ItemViewService;
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
    private final ItemViewService itemViewService;

    public ItemResponse view(Long itemId, Long userId) {
        // 상품 조회 이력 기록
        itemViewService.recordItemView(itemId, userId);
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

    public PopularItemsResponse viewPopular(PopularityPeriod period) {
        PopularItemsDto popularItemsDto = itemPopularityService.getPopularItems(period);
        return PopularItemsResponse.of(popularItemsDto.getItems());
    }
}
