package com.side.hhplusecommerce.item.service;

import com.side.hhplusecommerce.cart.domain.CartItem;
import com.side.hhplusecommerce.item.domain.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemStockService {
    private final ItemStockLockService itemStockLockService;

    @Transactional
    public void decreaseStock(List<CartItem> cartItems, List<Item> items) {
        for (CartItem cartItem : cartItems) {
            itemStockLockService.decreaseStockWithOptimisticLock(cartItem.getItemId(), cartItem.getQuantity());
        }
    }

    @Transactional
    public void increaseStock(List<CartItem> cartItems, List<Item> items) {
        for (CartItem cartItem : cartItems) {
            itemStockLockService.increaseStockWithOptimisticLock(cartItem.getItemId(), cartItem.getQuantity());
        }
    }
}
