package com.side.hhplusecommerce.item.repository;

import com.side.hhplusecommerce.item.domain.Item;

import java.util.Optional;

public interface ItemRepository {
    Optional<Item> findById(Long itemId);
}