package com.side.hhplusecommerce.cart.domain;

import com.side.hhplusecommerce.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class User extends BaseEntity {
    private Long userId;

    @Builder(access = AccessLevel.PRIVATE)
    private User(Long userId) {
        super();
        this.userId = userId;
    }

    public static User create(Long userId) {
        return User.builder()
                .userId(userId)
                .build();
    }

}
