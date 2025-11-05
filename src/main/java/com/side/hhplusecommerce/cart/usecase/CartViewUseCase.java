package com.side.hhplusecommerce.cart.usecase;

import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.repository.ItemRepository;
import com.side.hhplusecommerce.cart.controller.dto.CartResponse;
import com.side.hhplusecommerce.cart.domain.Cart;
import com.side.hhplusecommerce.cart.domain.CartItem;
import com.side.hhplusecommerce.cart.repository.CartItemRepository;
import com.side.hhplusecommerce.cart.repository.CartRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartViewUseCase {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;

    public CartResponse view(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElse(null);

        if (Objects.isNull(cart)) {
            return new CartResponse(
                    Collections.emptyList(),
                    new CartResponse.Summary(0, 0, 0)
            );
        }

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getCartId());

        if (cartItems.isEmpty()) {
            return new CartResponse(
                    Collections.emptyList(),
                    new CartResponse.Summary(0, 0, 0)
            );
        }

        List<Long> itemIds = cartItems.stream()
                .map(CartItem::getItemId)
                .toList();

        Map<Long, Item> itemMap = itemIds.stream()
                .map(itemRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(Item::getItemId, item -> item));

        return CartResponse.of(cartItems, itemMap);
    }
}
