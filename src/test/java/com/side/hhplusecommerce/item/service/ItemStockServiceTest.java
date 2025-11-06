package com.side.hhplusecommerce.item.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.side.hhplusecommerce.cart.domain.CartItem;
import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.repository.ItemRepository;
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
    private ItemRepository itemRepository;

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
                .salesCount(0)
                .build();

        List<CartItem> cartItems = List.of(cartItem);
        List<Item> items = List.of(item);

        // when
        itemStockService.decreaseStock(cartItems, items);

        // then
        assertThat(item.getStock()).isEqualTo(90);
        assertThat(item.getSalesCount()).isEqualTo(10);
        verify(itemRepository).save(item);
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
                .salesCount(salesCount)
                .build();

        List<CartItem> cartItems = List.of(cartItem);
        List<Item> items = List.of(item);

        // when & then
        assertThatThrownBy(() -> itemStockService.decreaseStock(cartItems, items))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INSUFFICIENT_STOCK.getMessage());

        assertThat(item.getStock()).isEqualTo(initialStock);
        assertThat(item.getSalesCount()).isEqualTo(salesCount);
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    @DisplayName("장바구니 항목에 해당하는 상품이 없으면 예외를 발생시킨다")
    void decreaseStock_fail_item_not_found() {
        // given
        CartItem cartItem1 = CartItem.createWithId(1L, 100L, 1L, 5);
        CartItem cartItem2 = CartItem.createWithId(2L, 100L, 2L, 3);

        Item item1 = Item.builder().itemId(1L).name("Item 1").price(10000).stock(100).salesCount(0).build();
        // item2가 없음

        List<CartItem> cartItems = List.of(cartItem1, cartItem2);
        List<Item> items = List.of(item1);

        // when & then
        assertThatThrownBy(() -> itemStockService.decreaseStock(cartItems, items))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.ITEM_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("여러 상품 중 일부의 재고가 부족하면 예외를 발생시킨다")
    void decreaseStock_fail_partial_insufficient_stock() {
        // given
        CartItem cartItem1 = CartItem.createWithId(1L, 100L, 1L, 5);
        CartItem cartItem2 = CartItem.createWithId(2L, 100L, 2L, 100);

        Item item1 = Item.builder().itemId(1L).name("Item 1").price(10000).stock(100).salesCount(0).build();
        Item item2 = Item.builder().itemId(2L).name("Item 2").price(20000).stock(50).salesCount(0).build();

        List<CartItem> cartItems = List.of(cartItem1, cartItem2);
        List<Item> items = List.of(item1, item2);

        Integer initialStock1 = item1.getStock();
        Integer initialStock2 = item2.getStock();

        // when & then
        assertThatThrownBy(() -> itemStockService.decreaseStock(cartItems, items))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INSUFFICIENT_STOCK.getMessage());

        // 첫 번째 상품은 차감되고, 두 번째 상품에서 예외 발생
        assertThat(item1.getStock()).isEqualTo(initialStock1 - 5);
        assertThat(item2.getStock()).isEqualTo(initialStock2);
    }

}
