package com.side.hhplusecommerce.user.controller;

import com.side.hhplusecommerce.user.controller.dto.CartItemRequest;
import com.side.hhplusecommerce.user.controller.dto.CartItemResponse;
import com.side.hhplusecommerce.user.controller.dto.CartResponse;
import com.side.hhplusecommerce.user.controller.dto.UpdateCartItemRequest;
import com.side.hhplusecommerce.user.usecase.CartAddUseCase;
import com.side.hhplusecommerce.user.usecase.CartUpdateUseCase;
import com.side.hhplusecommerce.user.usecase.CartViewUseCase;
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
    private final CartViewUseCase cartViewUseCase;
    private final CartUpdateUseCase cartUpdateUseCase;

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
        CartItemResponse response = cartUpdateUseCase.update(
                cartItemId,
                request.getUserId(),
                request.getQuantity()
        );
        return ResponseEntity.ok(response);
    }
}
