package com.side.hhplusecommerce.cart.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.side.hhplusecommerce.ContainerTest;
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

@SpringBootTest
@AutoConfigureMockMvc
class CartIntegrationTest extends ContainerTest {

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

    private Long testCartId;
    private Long testItemId1;
    private Long testItemId2;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비
        Item item1 = Item.builder()
                .name("Test Item 1")
                .price(10000)
                .stock(100)
                .salesCount(0)
                .build();

        Item item2 = Item.builder()
                .name("Test Item 2")
                .price(20000)
                .stock(50)
                .salesCount(0)
                .build();

        Item savedItem1 = itemRepository.save(item1);
        Item savedItem2 = itemRepository.save(item2);
        testItemId1 = savedItem1.getItemId();
        testItemId2 = savedItem2.getItemId();

        testUserId = 1L;
        Cart cart = Cart.builder()
                .userId(testUserId)
                .build();
        Cart savedCart = cartRepository.save(cart);
        testCartId = savedCart.getCartId();

        CartItem cartItem = CartItem.create(testCartId, testItemId1, 5);
        cartItemRepository.save(cartItem);
    }

    @Test
    @DisplayName("[성공] 장바구니에 상품 추가 - 새 장바구니 생성")
    void addCartItem_success_createNewCart() throws Exception {
        CartItemRequest request = new CartItemRequest(200L, testItemId2, 3);

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cartItemId").exists())
                .andExpect(jsonPath("$.itemId").value(testItemId2))
                .andExpect(jsonPath("$.quantity").value(3));
    }

    @Test
    @DisplayName("[성공] 장바구니에 상품 추가 - 기존 장바구니에 추가")
    void addCartItem_success_existingCart() throws Exception {
        CartItemRequest request = new CartItemRequest(testUserId, testItemId2, 3);

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cartItemId").exists());
    }

    @Test
    @DisplayName("[성공] 장바구니 조회")
    void getCart_success() throws Exception {
        mockMvc.perform(get("/api/cart")
                        .param("userId", String.valueOf(testUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].cartItemId").exists())
                .andExpect(jsonPath("$.items[0].itemId").value(testItemId1));
    }

    @Test
    @DisplayName("[성공] 장바구니 상품 수량 수정")
    void updateCartItemQuantity_success() throws Exception {
        CartItem existingCartItem = cartItemRepository.findByCartId(testCartId).stream()
                .filter(ci -> ci.getItemId().equals(testItemId1))
                .findFirst()
                .orElseThrow();
        int quantityToUpdate = existingCartItem.getQuantity() + 1;

        UpdateCartItemRequest request = new UpdateCartItemRequest(testUserId, quantityToUpdate);

        mockMvc.perform(patch("/api/cart/items/{cartItemId}", existingCartItem.getCartItemId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value(testItemId1))
                .andExpect(jsonPath("$.quantity").value(quantityToUpdate));
    }
}
