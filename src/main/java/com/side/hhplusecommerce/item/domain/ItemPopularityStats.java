package com.side.hhplusecommerce.item.domain;

import com.side.hhplusecommerce.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "item_popularity_stats", indexes = {
        @Index(name = "idx_item_popularity_stats_item_id", columnList = "item_id"),
        @Index(name = "idx_item_popularity_stats_popularity_score", columnList = "popularity_score DESC"),
        @Index(name = "idx_date_score", columnList = "based_on_date, popularity_score DESC")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemPopularityStats extends BaseEntity {

    private static final int VIEW_COUNT_WEIGHT = 1;
    private static final int SALES_COUNT_WEIGHT = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stats_id")
    private Long statsId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    @Column(name = "sales_count", nullable = false)
    private Long salesCount;

    @Column(name = "popularity_score", nullable = false)
    private Long popularityScore;

    @Column(name = "based_on_date", nullable = false)
    private LocalDateTime basedOnDate;

    @Builder(access = AccessLevel.PRIVATE)
    private ItemPopularityStats(Long statsId, Long itemId, Long viewCount, Long salesCount, LocalDateTime basedOnDate) {
        super();
        this.statsId = statsId;
        this.itemId = itemId;
        this.viewCount = viewCount;
        this.salesCount = salesCount;
        this.basedOnDate = basedOnDate;
        this.popularityScore = viewCount * VIEW_COUNT_WEIGHT + salesCount * SALES_COUNT_WEIGHT;
    }

    public static ItemPopularityStats create(Long itemId, Long viewCount, Long salesCount, LocalDateTime basedOnDate) {
        return ItemPopularityStats.builder()
                .itemId(itemId)
                .viewCount(viewCount)
                .salesCount(salesCount)
                .basedOnDate(basedOnDate)
                .build();
    }
}
