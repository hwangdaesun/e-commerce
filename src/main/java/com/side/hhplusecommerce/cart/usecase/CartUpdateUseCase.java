package com.side.hhplusecommerce.cart.usecase;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.repository.ItemRepository;
import com.side.hhplusecommerce.cart.controller.dto.CartItemResponse;
import com.side.hhplusecommerce.cart.domain.Cart;
import com.side.hhplusecommerce.cart.domain.CartItem;
import com.side.hhplusecommerce.cart.domain.CartItemValidator;
import com.side.hhplusecommerce.cart.repository.CartItemRepository;
import com.side.hhplusecommerce.cart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartUpdateUseCase {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;
    private final CartItemValidator cartItemValidator;

    public CartItemResponse update(Long cartItemId, Long userId, Integer quantity) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_ITEM_NOT_FOUND));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_ITEM_NOT_FOUND));

        cartItemValidator.validateOwnership(cartItem, cart);

        Item item = itemRepository.findById(cartItem.getItemId())
                .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));

        if (!item.hasEnoughQuantity(quantity)) {
            throw new CustomException(ErrorCode.INSUFFICIENT_STOCK);
        }

        cartItem.updateQuantity(quantity);
        CartItem updatedCartItem = cartItemRepository.save(cartItem);

        return CartItemResponse.of(updatedCartItem, item);
    }
}
