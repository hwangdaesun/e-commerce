package com.side.hhplusecommerce.user.controller;

import com.side.hhplusecommerce.user.controller.dto.CartItemRequest;
import com.side.hhplusecommerce.user.controller.dto.CartItemResponse;
import com.side.hhplusecommerce.user.controller.dto.CartResponse;
import com.side.hhplusecommerce.user.controller.dto.UpdateCartItemRequest;
import com.side.hhplusecommerce.user.usecase.CartAddUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController implements CartControllerDocs {
    private final CartAddUseCase cartAddUseCase;

    @Override
    @PostMapping("/items")
    public ResponseEntity<CartItemResponse> addCartItem(@RequestBody CartItemRequest request) {
        CartItemResponse response = cartAddUseCase.add(
                request.getUserId(),
                request.getItemId(),
                request.getQuantity()
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

    @Override
    @PatchMapping("/items/{cartItemId}")
    public ResponseEntity<CartItemResponse> updateCartItemQuantity(
            @PathVariable Long cartItemId,
            @RequestBody UpdateCartItemRequest request
    ) {
        // Mock 데이터
        CartItemResponse response = new CartItemResponse(
                1L,
                1L,
                "기본 티셔츠",
                29000,
                5,
                145000,
                50,
                LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
}
