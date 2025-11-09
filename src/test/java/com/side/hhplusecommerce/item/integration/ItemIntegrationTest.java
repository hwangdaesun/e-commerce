package com.side.hhplusecommerce.item.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ItemIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ItemRepository itemRepository;

    @BeforeEach
    void setUp() {
        // 모든 데이터 정리 (테스트 격리)
        itemRepository.deleteAll();

        // 테스트 데이터 준비
        Item item1 = Item.builder()
                .itemId(1L)
                .name("Test Item 1")
                .price(10000)
                .stock(100)
                .salesCount(50)
                .build();

        Item item2 = Item.builder()
                .itemId(2L)
                .name("Test Item 2")
                .price(20000)
                .stock(50)
                .salesCount(30)
                .build();

        itemRepository.save(item1);
        itemRepository.save(item2);
    }

    @Test
    @DisplayName("[성공] 상품 목록 조회 - 커서 없이")
    void getItems_success_withoutCursor() throws Exception {
        mockMvc.perform(get("/api/items")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.hasNext").exists());
    }

    @Test
    @DisplayName("[성공] 상품 목록 조회 - 커서 포함")
    void getItems_success_withCursor() throws Exception {
        mockMvc.perform(get("/api/items")
                        .param("cursor", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.hasNext").exists());
    }

    @Test
    @DisplayName("[성공] 상품 상세 조회")
    void getItem_success() throws Exception {
        mockMvc.perform(get("/api/items/{itemId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value(1))
                .andExpect(jsonPath("$.name").value("Test Item 1"))
                .andExpect(jsonPath("$.price").value(10000))
                .andExpect(jsonPath("$.stock").value(100));
    }

    @Test
    @DisplayName("[실패] 상품 상세 조회 - 존재하지 않는 상품")
    void getItem_fail_notFound() throws Exception {
        mockMvc.perform(get("/api/items/{itemId}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("[성공] 인기 상품 조회")
    void getPopularItems_success() throws Exception {
        mockMvc.perform(get("/api/items/popular")
                        .param("limit", "5"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("[성공] 상품 재고 조회")
    void getItemStock_success() throws Exception {
        mockMvc.perform(get("/api/items/{itemId}/stock", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value(1))
                .andExpect(jsonPath("$.stock").value(100));
    }

    @Test
    @DisplayName("[실패] 상품 재고 조회 - 존재하지 않는 상품")
    void getItemStock_fail_notFound() throws Exception {
        mockMvc.perform(get("/api/items/{itemId}/stock", 999L))
                .andExpect(status().isNotFound());
    }
}
