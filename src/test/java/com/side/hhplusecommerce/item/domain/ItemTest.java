package com.side.hhplusecommerce.item.domain;

import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.item.exception.InsufficientStockException;
import com.side.hhplusecommerce.item.exception.InvalidSalesQuantityException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class ItemTest {

    @Test
    @DisplayName("재고가 부족할 경우 재고 차감을 하지 않고 예외를 발생시킨다")
    void decrease_fail_insufficient_stock() {
        // given
        Item item = Item.builder()
                .itemId(1L)
                .name("테스트 상품")
                .price(10000)
                .stock(5)
                .build();

        // when & then
        assertThatThrownBy(() -> item.decrease(10))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage(ErrorCode.INSUFFICIENT_STOCK.getMessage());

        // 재고가 차감되지 않았는지 확인
        assertThat(item.getStock()).isEqualTo(5);
    }

    @Test
    @DisplayName("재고를 정상적으로 증가시킨다")
    void increase_success() {
        // given
        Item item = Item.builder()
                .itemId(1L)
                .name("테스트 상품")
                .price(10000)
                .stock(10)
                .build();

        // when
        item.increase(5);

        // then
        assertThat(item.getStock()).isEqualTo(15);
    }

    @Test
    @DisplayName("판매 수량을 정상적으로 증가시킨다")
    void increaseSalesCount_success() {
        // given
        Item item = Item.builder()
                .itemId(1L)
                .name("테스트 상품")
                .price(10000)
                .stock(10)
                .salesCount(5)
                .build();

        // when
        item.increaseSalesCount(3);

        // then
        assertThat(item.getSalesCount()).isEqualTo(8);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1, -10})
    @DisplayName("판매 수량이 null, 0 또는 음수일 경우 예외를 발생시킨다")
    void increaseSalesCount_fail_invalid_quantity(Integer quantity) {
        // given
        Item item = Item.builder()
                .itemId(1L)
                .name("테스트 상품")
                .price(10000)
                .stock(10)
                .salesCount(5)
                .build();

        // when & then
        assertThatThrownBy(() -> item.increaseSalesCount(quantity))
                .isInstanceOf(InvalidSalesQuantityException.class)
                .hasMessage(ErrorCode.INVALID_SALES_QUANTITY.getMessage());

        // 판매 수량이 변경되지 않았는지 확인
        assertThat(item.getSalesCount()).isEqualTo(5);
    }

}