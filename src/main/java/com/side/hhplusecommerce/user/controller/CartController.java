package com.side.hhplusecommerce.user.controller;

import com.side.hhplusecommerce.user.controller.dto.CartItemRequest;
import com.side.hhplusecommerce.user.controller.dto.CartItemResponse;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController implements CartControllerDocs {

    @Override
    @PostMapping("/items")
    public ResponseEntity<CartItemResponse> addCartItem(@RequestBody CartItemRequest request) {

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

}
