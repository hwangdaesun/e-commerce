package com.side.hhplusecommerce.item.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.item.repository.ItemRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemValidatorTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemValidator itemValidator;

    @Test
    @DisplayName("단일 상품 ID로 검증 시 존재하면 상품을 반환한다")
    void validateExistence_single_success() {
        // given
        Long itemId = 1L;
        Item item = Item.builder()
                .itemId(itemId)
                .name("Test Item")
                .price(10000)
                .stock(100)
                .build();

        given(itemRepository.findById(itemId)).willReturn(Optional.of(item));

        // when
        Item result = itemValidator.validateExistence(itemId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getItemId()).isEqualTo(itemId);
        assertThat(result.getName()).isEqualTo("Test Item");
    }

    @Test
    @DisplayName("단일 상품 ID로 검증 시 존재하지 않으면 예외를 발생시킨다")
    void validateExistence_single_fail_not_found() {
        // given
        Long itemId = 1L;
        given(itemRepository.findById(itemId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> itemValidator.validateExistence(itemId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.ITEM_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("여러 상품 ID로 검증 시 모두 존재하면 상품 목록을 반환한다")
    void validateExistence_multiple_success() {
        // given
        List<Long> itemIds = List.of(1L, 2L, 3L);
        List<Item> items = List.of(
                Item.builder().itemId(1L).name("Item 1").price(10000).stock(100).build(),
                Item.builder().itemId(2L).name("Item 2").price(20000).stock(200).build(),
                Item.builder().itemId(3L).name("Item 3").price(30000).stock(300).build()
        );

        given(itemRepository.findAllByIds(itemIds)).willReturn(items);

        // when
        List<Item> result = itemValidator.validateExistence(itemIds);

        // then
        assertThat(result).hasSize(3);
        assertThat(result).extracting(Item::getItemId)
                .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    @DisplayName("여러 상품 ID로 검증 시 일부만 존재하면 예외를 발생시킨다")
    void validateExistence_multiple_fail_partial() {
        // given
        List<Long> itemIds = List.of(1L, 2L, 3L);
        List<Item> items = List.of(
                Item.builder().itemId(1L).name("Item 1").price(10000).stock(100).build(),
                Item.builder().itemId(2L).name("Item 2").price(20000).stock(200).build()
        );

        given(itemRepository.findAllByIds(itemIds)).willReturn(items);

        // when & then
        assertThatThrownBy(() -> itemValidator.validateExistence(itemIds))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.ITEM_NOT_FOUND.getMessage());
    }

}
