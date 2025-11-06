package com.side.hhplusecommerce.item.service;

import com.side.hhplusecommerce.cart.domain.CartItem;
import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemStockService {
    private final ItemRepository itemRepository;

    public void decreaseStock(List<CartItem> cartItems, List<Item> items) {
        Map<Long, Item> itemMap = items.stream()
                .collect(Collectors.toMap(Item::getItemId, item -> item));

        for (CartItem cartItem : cartItems) {
            if(itemMap.containsKey(cartItem.getItemId())){
                Item item = itemMap.get(cartItem.getItemId());
                item.decrease(cartItem.getQuantity());
                item.increaseSalesCount(cartItem.getQuantity());
                itemRepository.save(item);
            }
        }
    }

    public void increaseStock(List<CartItem> cartItems, List<Item> items) {
        Map<Long, Item> itemMap = items.stream()
                .collect(Collectors.toMap(Item::getItemId, item -> item));

        for (CartItem cartItem : cartItems) {
            Item item = itemMap.get(cartItem.getItemId());
            if (item != null) {
                item.increase(cartItem.getQuantity());
                item.decreaseSalesCount(cartItem.getQuantity());
                itemRepository.save(item);
            }
        }
    }
}
