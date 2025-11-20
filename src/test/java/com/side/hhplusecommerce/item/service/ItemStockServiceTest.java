package com.side.hhplusecommerce.item.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.side.hhplusecommerce.cart.domain.CartItem;
import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.item.domain.Item;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemStockServiceTest {

    @Mock
    private ItemStockLockService itemStockLockService;

    @InjectMocks
    private ItemStockService itemStockService;

    @Test
    @DisplayName("재고를 차감하고 판매 수량을 증가시킨다")
    void decreaseStock_success() {
        // given
        Integer initialStock = 100;
        Integer orderQuantity = 10;

        CartItem cartItem = CartItem.createWithId(1L, 100L, 1L, orderQuantity);
        Item item = Item.builder()
                .itemId(1L)
                .name("Item 1")
                .price(10000)
                .stock(initialStock)
                .build();

        List<CartItem> cartItems = List.of(cartItem);
        List<Item> items = List.of(item);

        // when
        itemStockService.decreaseStock(cartItems, items);

        // then
        verify(itemStockLockService).decreaseStockWithPessimisticLock(1L, orderQuantity);
    }

    @Test
    @DisplayName("재고가 부족하면 예외를 발생시킨다")
    void decreaseStock_fail_insufficient_stock() {
        // given
        Integer initialStock = 5;
        Integer orderQuantity = 10;
        Integer salesCount = 5;

        CartItem cartItem = CartItem.createWithId(1L, 100L, 1L, orderQuantity);
        Item item = Item.builder()
                .itemId(1L)
                .name("Item 1")
                .price(10000)
                .stock(initialStock)
                .build();

        List<CartItem> cartItems = List.of(cartItem);
        List<Item> items = List.of(item);

        doThrow(new CustomException(ErrorCode.INSUFFICIENT_STOCK))
                .when(itemStockLockService).decreaseStockWithPessimisticLock(1L, orderQuantity);

        // when & then
        assertThatThrownBy(() -> itemStockService.decreaseStock(cartItems, items))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INSUFFICIENT_STOCK.getMessage());

        verify(itemStockLockService).decreaseStockWithPessimisticLock(1L, orderQuantity);
    }

}
