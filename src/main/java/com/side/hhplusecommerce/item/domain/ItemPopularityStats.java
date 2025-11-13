package com.side.hhplusecommerce.item.domain;

import com.side.hhplusecommerce.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "item_popularity_stats", indexes = {
        @Index(name = "idx_item_popularity_stats_item_id", columnList = "item_id"),
        @Index(name = "idx_item_popularity_stats_popularity_score", columnList = "popularity_score DESC")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemPopularityStats extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stats_id")
    private Long statsId;

    @Column(name = "item_id", nullable = false, unique = true)
    private Long itemId;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    @Column(name = "sales_count", nullable = false)
    private Long salesCount;

    @Column(name = "popularity_score", nullable = false)
    private Long popularityScore;

    @Builder
    private ItemPopularityStats(Long statsId, Long itemId, Long viewCount, Long salesCount, Long popularityScore) {
        super();
        this.statsId = statsId;
        this.itemId = itemId;
        this.viewCount = viewCount;
        this.salesCount = salesCount;
        this.popularityScore = popularityScore;
    }

    public void updateStats(Long viewCount, Long salesCount, Long popularityScore) {
        this.viewCount = viewCount;
        this.salesCount = salesCount;
        this.popularityScore = popularityScore;
    }
}