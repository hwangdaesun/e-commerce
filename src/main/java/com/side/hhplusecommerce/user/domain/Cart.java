package com.side.hhplusecommerce.user.domain;

import com.side.hhplusecommerce.common.BaseEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
public class Cart extends BaseEntity {
    private Long cartId;
    private Long userId;

    @Builder
    private Cart(Long cartId, Long userId) {
        super();
        this.cartId = cartId;
        this.userId = userId;
    }
}