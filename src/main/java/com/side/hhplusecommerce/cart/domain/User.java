package com.side.hhplusecommerce.cart.domain;

import com.side.hhplusecommerce.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Generated;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
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
