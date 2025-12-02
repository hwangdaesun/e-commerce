package com.side.hhplusecommerce.item.service;

import com.side.hhplusecommerce.item.domain.ItemView;
import com.side.hhplusecommerce.item.repository.ItemViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemViewService {

    private final ItemViewRepository itemViewRepository;
    private final ItemPopularityService itemPopularityService;

    /**
     * 상품 조회 이력을 저장하고, Redis ZSET의 score를 업데이트합니다.
     *
     * @param itemId 상품 ID
     * @param userId 사용자 ID
     * @return 저장된 ItemView 엔티티
     */
    @Transactional
    public ItemView recordItemView(Long itemId, Long userId) {
        ItemView itemView = ItemView.builder()
                .itemId(itemId)
                .userId(userId)
                .build();

        ItemView savedItemView = itemViewRepository.save(itemView);
        log.debug("Item view recorded - itemId: {}, userId: {}, itemViewId: {}",
                itemId, userId, savedItemView.getItemViewId());

        // Redis ZSET score 업데이트 (조회수 가중치 적용)
        itemPopularityService.incrementViewScore(itemId);

        return savedItemView;
    }
}
