package com.side.hhplusecommerce.item.service;

import com.side.hhplusecommerce.cart.domain.CartItem;
import com.side.hhplusecommerce.item.domain.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemStockService {
    private final ItemStockLockService itemStockLockService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void decreaseStock(List<CartItem> cartItems, List<Item> items) {
        for (CartItem cartItem : cartItems) {
            itemStockLockService.decreaseStockWithPessimisticLock(cartItem.getItemId(), cartItem.getQuantity());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void increaseStock(List<CartItem> cartItems, List<Item> items) {
        for (CartItem cartItem : cartItems) {
            itemStockLockService.increaseStockWithWithPessimisticLock(cartItem.getItemId(), cartItem.getQuantity());
        }
    }
}
