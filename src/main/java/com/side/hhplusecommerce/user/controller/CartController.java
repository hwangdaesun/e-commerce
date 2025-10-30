package com.side.hhplusecommerce.user.controller;

import com.side.hhplusecommerce.user.controller.dto.CartItemRequest;
import com.side.hhplusecommerce.user.controller.dto.CartItemResponse;
import java.time.LocalDateTime;
import com.side.hhplusecommerce.user.controller.dto.CartResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController implements CartControllerDocs {

    @Override
    @PostMapping("/items")
    public ResponseEntity<CartItemResponse> addCartItem(@RequestBody CartItemRequest request) {
        // Mock 데이터
        CartItemResponse response = new CartItemResponse(
                1L,
                1L,
                "기본 티셔츠",
                29000,
                2,
                58000,
                50,
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @GetMapping
    public ResponseEntity<CartResponse> getCart(@RequestParam Long userId) {
        // Mock 데이터
        List<CartResponse.CartItem> items = List.of(
                new CartResponse.CartItem(1L, 1L, "기본 티셔츠", 29000, 2, 58000, 50),
                new CartResponse.CartItem(2L, 2L, "청바지", 59000, 1, 59000, 30)
        );

        CartResponse.Summary summary = new CartResponse.Summary(2, 3, 117000);
        CartResponse response = new CartResponse(items, summary);

        return ResponseEntity.ok(response);
    }
}
