package com.side.hhplusecommerce.item.controller;

import com.side.hhplusecommerce.common.dto.CursorRequest;
import com.side.hhplusecommerce.item.constants.PopularityPeriod;
import com.side.hhplusecommerce.item.controller.dto.ItemResponse;
import com.side.hhplusecommerce.item.controller.dto.ItemStockResponse;
import com.side.hhplusecommerce.item.controller.dto.ItemsResponse;
import com.side.hhplusecommerce.item.controller.dto.PopularItemsResponse;
import com.side.hhplusecommerce.item.usecase.ItemViewUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController implements ItemControllerDocs {
    private final ItemViewUseCase itemViewUseCase;

    @Override
    @GetMapping
    public ResponseEntity<ItemsResponse> getItems(
            @RequestParam(required = false, defaultValue = "1") Long cursor,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        CursorRequest cursorRequest = CursorRequest.of(cursor, size);
        ItemsResponse response = itemViewUseCase.view(cursorRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/{itemId}")
    public ResponseEntity<ItemResponse> getItem(
            @PathVariable Long itemId,
            @RequestParam(required = true) Long userId
    ) {
        ItemResponse response = itemViewUseCase.view(itemId, userId);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/popular")
    public ResponseEntity<PopularItemsResponse> getPopularItems(
            @RequestParam(defaultValue = "WEEKLY") PopularityPeriod period
    ) {
        PopularItemsResponse response = itemViewUseCase.viewPopular(period);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/{itemId}/stock")
    public ResponseEntity<ItemStockResponse> getItemStock(
            @PathVariable Long itemId
    ) {
        ItemStockResponse response = itemViewUseCase.viewStock(itemId);
        return ResponseEntity.ok(response);
    }
}
