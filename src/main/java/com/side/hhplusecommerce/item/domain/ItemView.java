package com.side.hhplusecommerce.item.domain;

import com.side.hhplusecommerce.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "item_views", indexes = {
        @Index(name = "idx_item_views_item_id", columnList = "item_id"),
        @Index(name = "idx_item_views_user_id", columnList = "user_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemView extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_view_id")
    private Long itemViewId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Builder
    private ItemView(Long itemViewId, Long itemId, Long userId) {
        super();
        this.itemViewId = itemViewId;
        this.itemId = itemId;
        this.userId = userId;
    }
}
