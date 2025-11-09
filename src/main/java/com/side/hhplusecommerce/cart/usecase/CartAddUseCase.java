package com.side.hhplusecommerce.cart.usecase;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.domain.ItemValidator;
import com.side.hhplusecommerce.cart.controller.dto.CartItemResponse;
import com.side.hhplusecommerce.cart.domain.Cart;
import com.side.hhplusecommerce.cart.domain.CartItem;
import com.side.hhplusecommerce.cart.repository.CartItemRepository;
import com.side.hhplusecommerce.cart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartAddUseCase {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ItemValidator itemValidator;

    public CartItemResponse add(Long userId, Long itemId, Integer quantity) {
        Item item = itemValidator.validateExistence(itemId);

        if (!item.hasEnoughQuantity(quantity)) {
            throw new CustomException(ErrorCode.INSUFFICIENT_STOCK);
        }

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(Cart.builder()
                        .userId(userId)
                        .build()));

        CartItem cartItem = CartItem.create(cart.getCartId(), itemId, quantity);
        CartItem savedCartItem = cartItemRepository.save(cartItem);

        return CartItemResponse.of(savedCartItem, item);
    }
}
