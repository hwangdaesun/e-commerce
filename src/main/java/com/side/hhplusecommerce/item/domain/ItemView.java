package com.side.hhplusecommerce.item.domain;

import com.side.hhplusecommerce.common.BaseEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
public class ItemView extends BaseEntity {
    private Long itemViewId;
    private Long itemId;
    private Long userId;

    @Builder
    private ItemView(Long itemViewId, Long itemId, Long userId) {
        super();
        this.itemViewId = itemViewId;
        this.itemId = itemId;
        this.userId = userId;
    }
}
