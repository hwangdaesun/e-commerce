package com.side.hhplusecommerce.item.controller;

import com.side.hhplusecommerce.item.controller.dto.ItemsResponse;
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

}
