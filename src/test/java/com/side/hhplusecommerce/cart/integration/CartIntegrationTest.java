package com.side.hhplusecommerce.cart.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.side.hhplusecommerce.cart.controller.dto.CartItemRequest;
import com.side.hhplusecommerce.cart.controller.dto.UpdateCartItemRequest;
import com.side.hhplusecommerce.cart.domain.Cart;
import com.side.hhplusecommerce.cart.domain.CartItem;
import com.side.hhplusecommerce.cart.repository.CartItemRepository;
import com.side.hhplusecommerce.cart.repository.CartRepository;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CartIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @BeforeEach
    void setUp() {
        // 모든 데이터 정리 (테스트 격리)
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        itemRepository.deleteAll();

        // 테스트 데이터 준비
        Item item1 = Item.builder()
                .itemId(1L)
                .name("Test Item 1")
                .price(10000)
                .stock(100)
                .salesCount(0)
                .build();

        Item item2 = Item.builder()
                .itemId(2L)
                .name("Test Item 2")
                .price(20000)
                .stock(50)
                .salesCount(0)
                .build();

        itemRepository.save(item1);
        itemRepository.save(item2);

        Cart cart = Cart.builder()
                .cartId(1L)
                .userId(100L)
                .build();
        cartRepository.save(cart);

        CartItem cartItem = CartItem.create(cart.getCartId(), 1L, 5);
        cartItemRepository.save(cartItem);
    }

    @Test
    @DisplayName("[성공] 장바구니에 상품 추가 - 새 장바구니 생성")
    void addCartItem_success_createNewCart() throws Exception {
        CartItemRequest request = new CartItemRequest(200L, 2L, 3);

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cartItemId").exists())
                .andExpect(jsonPath("$.itemId").value(2))
                .andExpect(jsonPath("$.quantity").value(3));
    }

    @Test
    @DisplayName("[성공] 장바구니에 상품 추가 - 기존 장바구니에 추가")
    void addCartItem_success_existingCart() throws Exception {
        CartItemRequest request = new CartItemRequest(100L, 2L, 3);

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cartItemId").exists())
                .andExpect(jsonPath("$.itemId").value(2))
                .andExpect(jsonPath("$.quantity").value(3));
    }

    @Test
    @DisplayName("[성공] 장바구니 조회")
    void getCart_success() throws Exception {
        mockMvc.perform(get("/api/cart")
                        .param("userId", "100"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("[성공] 장바구니 상품 수량 수정")
    void updateCartItemQuantity_success() throws Exception {
        CartItem existingCartItem = cartItemRepository.findByCartId(1L).stream()
                .filter(ci -> ci.getItemId() == 1L)
                .findFirst()
                .orElseThrow();

        UpdateCartItemRequest request = new UpdateCartItemRequest(100L, 10);

        mockMvc.perform(patch("/api/cart/items/{cartItemId}", existingCartItem.getCartItemId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(10));
    }
}
