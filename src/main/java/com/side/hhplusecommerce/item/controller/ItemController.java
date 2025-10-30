package com.side.hhplusecommerce.item.controller;

import com.side.hhplusecommerce.item.controller.dto.ItemResponse;
import com.side.hhplusecommerce.item.controller.dto.ItemsResponse;
import com.side.hhplusecommerce.item.controller.dto.PopularItemsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/items")
public class ItemController implements ItemControllerDocs {

    @Override
    @GetMapping
    public ResponseEntity<ItemsResponse> getItems(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        // Mock 데이터
        List<ItemsResponse.ItemInfo> items = List.of(
                new ItemsResponse.ItemInfo(1L, "기본 티셔츠", 29000, 0, LocalDateTime.now()),
                new ItemsResponse.ItemInfo(2L, "청바지", 59000, 50, LocalDateTime.now())
        );

        ItemsResponse response = new ItemsResponse(items, null, false);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/{itemId}")
    public ResponseEntity<ItemResponse> getItem(
            @PathVariable Long itemId
    ) {
        // Mock 데이터
        ItemResponse response = new ItemResponse(
                1L,
                "기본 티셔츠",
                29000,
                50,
                LocalDateTime.now()
        );
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
}
