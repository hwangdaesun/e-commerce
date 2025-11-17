package com.side.hhplusecommerce.item.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.side.hhplusecommerce.ContainerTest;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.domain.ItemView;
import com.side.hhplusecommerce.item.repository.ItemRepository;
import com.side.hhplusecommerce.item.repository.ItemViewRepository;
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
class ItemIntegrationTest extends ContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemViewRepository itemViewRepository;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비
        Item item1 = Item.builder()
                .name("Test Item 1")
                .price(10000)
                .stock(100)
                .build();

        Item item2 = Item.builder()
                .name("Test Item 2")
                .price(20000)
                .stock(50)
                .build();

        Item savedItem1 = itemRepository.save(item1);
        Item savedItem2 = itemRepository.save(item2);

        // ItemView 데이터 추가
        ItemView itemView1 = ItemView.builder()
                .itemId(savedItem1.getItemId())
                .userId(1L)
                .build();

        ItemView itemView2 = ItemView.builder()
                .itemId(savedItem1.getItemId())
                .userId(2L)
                .build();

        ItemView itemView3 = ItemView.builder()
                .itemId(savedItem2.getItemId())
                .userId(1L)
                .build();

        itemViewRepository.save(itemView1);
        itemViewRepository.save(itemView2);
        itemViewRepository.save(itemView3);
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
        Item item = itemRepository.findAll().get(0);
        mockMvc.perform(get("/api/items/{itemId}", item.getItemId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value(item.getItemId()))
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
