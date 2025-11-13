package com.side.hhplusecommerce.item.repository;

import com.side.hhplusecommerce.item.domain.ItemPopularityStats;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItemPopularityStatsRepository extends JpaRepository<ItemPopularityStats, Long> {
    Optional<ItemPopularityStats> findByItemId(Long itemId);

    @Query("SELECT ips FROM ItemPopularityStats ips ORDER BY ips.popularityScore DESC")
    List<ItemPopularityStats> findTopByPopularityScore(Pageable pageable);

    @Query("SELECT ips FROM ItemPopularityStats ips " +
            "WHERE ips.basedOnDate = (SELECT MAX(ips2.basedOnDate) FROM ItemPopularityStats ips2) " +
            "ORDER BY ips.popularityScore DESC")
    List<ItemPopularityStats> findTopByLatestBasedOnDate(Pageable pageable);
}