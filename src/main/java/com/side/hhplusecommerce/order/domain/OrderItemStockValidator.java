package com.side.hhplusecommerce.order.domain;

import com.side.hhplusecommerce.cart.domain.CartItem;
import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.item.domain.Item;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderItemStockValidator {

    public void validateStock(List<CartItem> cartItems, List<Item> items) {
        for (CartItem cartItem : cartItems) {
            Item item = items.stream().filter(it -> it.getItemId().equals(cartItem.getItemId())).findFirst()
                    .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));

            if (!item.hasEnoughQuantity(cartItem.getQuantity())) {
                throw new CustomException(ErrorCode.INSUFFICIENT_STOCK);
            }
        }
    }
}
