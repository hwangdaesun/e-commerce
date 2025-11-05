package com.side.hhplusecommerce.cart.service;

import com.side.hhplusecommerce.cart.domain.CartItem;
import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.item.domain.Item;
import java.util.Objects;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CartItemService {

    public Integer calculateTotalAmount(List<CartItem> cartItems, List<Item> items) {
        Map<Long, Item> itemMap = items.stream()
                .collect(Collectors.toMap(Item::getItemId, item -> item));

        return cartItems.stream()
                .mapToInt(cartItem -> {
                    Item item = itemMap.get(cartItem.getItemId());
                    if (Objects.isNull(item)) {
                        throw new CustomException(ErrorCode.ITEM_NOT_FOUND);
                    }
                    return cartItem.calculateTotalPrice(item.getPrice());
                })
                .sum();
    }
}
