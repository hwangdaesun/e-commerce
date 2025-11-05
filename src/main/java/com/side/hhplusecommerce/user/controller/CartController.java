package com.side.hhplusecommerce.user.controller;

import com.side.hhplusecommerce.user.controller.dto.CartItemRequest;
import com.side.hhplusecommerce.user.controller.dto.CartItemResponse;
import com.side.hhplusecommerce.user.controller.dto.CartResponse;
import com.side.hhplusecommerce.user.controller.dto.UpdateCartItemRequest;
import com.side.hhplusecommerce.user.usecase.CartAddUseCase;
import com.side.hhplusecommerce.user.usecase.CartViewUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController implements CartControllerDocs {
    private final CartAddUseCase cartAddUseCase;
    private final CartViewUseCase cartViewUseCase;

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
        CartResponse response = cartViewUseCase.view(userId);
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
