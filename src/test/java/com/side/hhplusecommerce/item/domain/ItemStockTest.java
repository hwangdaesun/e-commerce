package com.side.hhplusecommerce.item.domain;

import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.item.exception.InsufficientStockException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ItemStockTest {

    @Test
    @DisplayName("재고가 부족할 경우 재고 차감을 하지 않고 예외를 발생시킨다")
    void decrease_fail_insufficient_stock() {
        // given
        ItemStock itemStock = ItemStock.of(1L, 5);

        // when & then
        assertThatThrownBy(() -> itemStock.decrease(10))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage(ErrorCode.INSUFFICIENT_STOCK.getMessage());

        // 재고가 차감되지 않았는지 확인
        assertThat(itemStock.getStock()).isEqualTo(5);
    }
}
