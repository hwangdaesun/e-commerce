package com.side.hhplusecommerce.user.usecase;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.repository.ItemRepository;
import com.side.hhplusecommerce.user.controller.dto.CartItemResponse;
import com.side.hhplusecommerce.user.domain.Cart;
import com.side.hhplusecommerce.user.domain.CartItem;
import com.side.hhplusecommerce.user.repository.CartItemRepository;
import com.side.hhplusecommerce.user.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartAddUseCase {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;

    public CartItemResponse add(Long userId, Long itemId, Integer quantity) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));

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
