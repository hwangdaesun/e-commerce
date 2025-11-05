package com.side.hhplusecommerce.item.controller;

import com.side.hhplusecommerce.common.dto.CursorRequest;
import com.side.hhplusecommerce.item.controller.dto.ItemResponse;
import com.side.hhplusecommerce.item.controller.dto.ItemStockResponse;
import com.side.hhplusecommerce.item.controller.dto.ItemsResponse;
import com.side.hhplusecommerce.item.controller.dto.PopularItemsResponse;
import com.side.hhplusecommerce.item.usecase.ItemViewUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController implements ItemControllerDocs {
    private final ItemViewUseCase itemViewUseCase;

    @Override
    @GetMapping
    public ResponseEntity<ItemsResponse> getItems(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        CursorRequest cursorRequest = CursorRequest.of(cursor, size);
        ItemsResponse response = itemViewUseCase.view(cursorRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/{itemId}")
    public ResponseEntity<ItemResponse> getItem(
            @PathVariable Long itemId
    ) {
        ItemResponse response = itemViewUseCase.view(itemId);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/popular")
    public ResponseEntity<PopularItemsResponse> getPopularItems(
            @RequestParam(defaultValue = "5") Integer limit
    ) {
        // Mock 데이터
        List<PopularItemsResponse.PopularItem> popularItems = List.of(
                new PopularItemsResponse.PopularItem(1, 1L, "기본 티셔츠", 29000, 50),
                new PopularItemsResponse.PopularItem(2, 2L, "청바지", 59000, 30),
                new PopularItemsResponse.PopularItem(3, 3L, "후드티", 45000, 20)
        );

        PopularItemsResponse response = new PopularItemsResponse(popularItems);
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
