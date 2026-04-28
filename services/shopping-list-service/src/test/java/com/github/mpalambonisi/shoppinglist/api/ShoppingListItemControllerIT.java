package com.github.mpalambonisi.shoppinglist.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mpalambonisi.shoppinglist.ShoppingListApplication;
import com.github.mpalambonisi.shoppinglist.client.ProductClient;
import com.github.mpalambonisi.shoppinglist.client.ProductResponse;
import com.github.mpalambonisi.shoppinglist.dto.request.AddProductToListRequest;
import com.github.mpalambonisi.shoppinglist.dto.request.CreateShoppingListRequest;
import com.github.mpalambonisi.shoppinglist.dto.request.UpdateQuantityRequest;
import com.github.mpalambonisi.shoppinglist.repository.ShoppingListItemRepository;
import com.github.mpalambonisi.shoppinglist.repository.ShoppingListRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for the shopping list item endpoints.
 *
 * <p>Design decisions:
 *
 * <ul>
 *   <li>{@code @MockBean ProductClient} replaces the real HTTP client with a Mockito mock so tests
 *       are fully self-contained — no product-service needs to be running. Tests that exercise item
 *       addition configure the mock to return a fake {@link ProductResponse} via {@code
 *       when(productClient.getProductById(PRODUCT_ID)).thenReturn(fakeProduct)}.
 *   <li>A list is created fresh per test via {@code performCreateList()} so item-level tests always
 *       have a valid parent list without cross-test dependencies.
 *   <li>The {@code X-Authenticated-User} header is sent on every request that belongs to a user,
 *       mirroring what the API gateway stamps on authenticated requests in production.
 *   <li>Two product IDs are defined ({@code PRODUCT_ID} and {@code SECOND_PRODUCT_ID}) to support
 *       tests that need multiple distinct items in a list.
 * </ul>
 */
@SpringBootTest(classes = ShoppingListApplication.class)
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Shopping List Item Controller Integration Tests")
public class ShoppingListItemControllerIT {

  private static final String AUTH_HEADER = "X-Authenticated-User";
  private static final String TEST_USER = "nicole.smith@example.com";
  private static final Long PRODUCT_ID = 101L;
  private static final Long SECOND_PRODUCT_ID = 102L;

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:15-alpine")
          .withDatabaseName("testdb")
          .withUsername("test")
          .withPassword("test");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    // Satisfies @Value injection in ProductClient at context startup.
    // The bean itself is mocked so this URL is never actually called.
    registry.add("services.product-service.url", () -> "http://localhost:9999");
  }

  @MockBean private ProductClient productClient;

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private ShoppingListRepository shoppingListRepository;
  @Autowired private ShoppingListItemRepository shoppingListItemRepository;

  @BeforeEach
  void setUp() {
    // Delete items first — they reference lists via FK
    shoppingListItemRepository.deleteAll();
    shoppingListRepository.deleteAll();

    // Default mock: PRODUCT_ID returns a valid product.
    // Individual tests can override this with their own when(...).thenReturn(...)
    when(productClient.getProductById(eq(PRODUCT_ID)))
        .thenReturn(buildFakeProduct(PRODUCT_ID, "Pomidory", new BigDecimal("5.99"), "kg"));

    when(productClient.getProductById(eq(SECOND_PRODUCT_ID)))
        .thenReturn(buildFakeProduct(SECOND_PRODUCT_ID, "Cebula", new BigDecimal("3.49"), "kg"));
  }

  @Test
  @DisplayName("Should add product to shopping list successfully")
  void shouldAddProductToShoppingListSuccessfully() throws Exception {
    String listId = performCreateList("Weekly Shopping");

    AddProductToListRequest request =
        AddProductToListRequest.builder().productId(PRODUCT_ID).quantity(3).build();

    mockMvc
        .perform(
            post("/api/shopping-lists/" + listId + "/items")
                .header(AUTH_HEADER, TEST_USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.listItemId").exists())
        .andExpect(jsonPath("$.productId").value(PRODUCT_ID))
        .andExpect(jsonPath("$.productName").value("Pomidory"))
        .andExpect(jsonPath("$.quantity").value(3))
        .andExpect(jsonPath("$.unit").value("kg"))
        .andExpect(jsonPath("$.priceAtAddition").value(5.99))
        .andExpect(jsonPath("$.isChecked").value(false))
        .andExpect(jsonPath("$.addedAt").exists());

    assertThat(shoppingListItemRepository.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should snapshot product price at time of addition")
  void shouldSnapshotProductPriceAtAddition() throws Exception {
    // Price at add time is 5.99
    String listId = performCreateList("Price Snapshot Test");
    String itemId = performAddItem(listId, PRODUCT_ID, 1);

    // Now the product price changes in product-service
    when(productClient.getProductById(eq(PRODUCT_ID)))
        .thenReturn(buildFakeProduct(PRODUCT_ID, "Pomidory", new BigDecimal("7.99"), "kg"));

    // The stored item should still show the original price
    mockMvc
        .perform(get("/api/shopping-lists/" + listId + "/items"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].priceAtAddition").value(5.99));
  }

  @Test
  @DisplayName("Should return 404 when adding item to non-existent list")
  void shouldReturnNotFoundWhenAddingItemToNonExistentList() throws Exception {
    AddProductToListRequest request =
        AddProductToListRequest.builder().productId(PRODUCT_ID).quantity(1).build();

    mockMvc
        .perform(
            post("/api/shopping-lists/99999/items")
                .header(AUTH_HEADER, TEST_USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").isArray());
  }

  @Test
  @DisplayName("Should return 404 when product does not exist in product-service")
  void shouldReturnNotFoundWhenProductDoesNotExist() throws Exception {
    String listId = performCreateList("My List");
    Long nonExistentProductId = 99999L;

    // product-service returns 404 — ProductClient translates this to ResourceNotFoundException
    when(productClient.getProductById(eq(nonExistentProductId)))
        .thenThrow(
            new com.github.mpalambonisi.common.exception.ResourceNotFoundException(
                "Product not found with id: " + nonExistentProductId));

    AddProductToListRequest request =
        AddProductToListRequest.builder().productId(nonExistentProductId).quantity(1).build();

    mockMvc
        .perform(
            post("/api/shopping-lists/" + listId + "/items")
                .header(AUTH_HEADER, TEST_USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").isArray());
  }

  @Test
  @DisplayName("Should return 400 when adding duplicate product to list")
  void shouldReturnBadRequestWhenAddingDuplicateProduct() throws Exception {
    String listId = performCreateList("My List");

    // Add the product once
    performAddItem(listId, PRODUCT_ID, 2);

    // Try to add the same product again
    AddProductToListRequest request =
        AddProductToListRequest.builder().productId(PRODUCT_ID).quantity(1).build();

    mockMvc
        .perform(
            post("/api/shopping-lists/" + listId + "/items")
                .header(AUTH_HEADER, TEST_USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message[0]").value("Product is already in this shopping list"));
  }

  @Test
  @DisplayName("Should return 400 when quantity is zero")
  void shouldReturnBadRequestWhenQuantityIsZero() throws Exception {
    String listId = performCreateList("My List");

    AddProductToListRequest request =
        AddProductToListRequest.builder().productId(PRODUCT_ID).quantity(0).build();

    mockMvc
        .perform(
            post("/api/shopping-lists/" + listId + "/items")
                .header(AUTH_HEADER, TEST_USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 when quantity is negative")
  void shouldReturnBadRequestWhenQuantityIsNegative() throws Exception {
    String listId = performCreateList("My List");

    AddProductToListRequest request =
        AddProductToListRequest.builder().productId(PRODUCT_ID).quantity(-5).build();

    mockMvc
        .perform(
            post("/api/shopping-lists/" + listId + "/items")
                .header(AUTH_HEADER, TEST_USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should get all items in a shopping list")
  void shouldGetAllItemsInShoppingList() throws Exception {
    String listId = performCreateList("My List");
    performAddItem(listId, PRODUCT_ID, 2);
    performAddItem(listId, SECOND_PRODUCT_ID, 1);

    mockMvc
        .perform(get("/api/shopping-lists/" + listId + "/items"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].productName").value("Pomidory"))
        .andExpect(jsonPath("$[1].productName").value("Cebula"));
  }

  @Test
  @DisplayName("Should return empty list when no items in shopping list")
  void shouldReturnEmptyListWhenNoItems() throws Exception {
    String listId = performCreateList("Empty List");

    mockMvc
        .perform(get("/api/shopping-lists/" + listId + "/items"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @DisplayName("Should update item quantity successfully")
  void shouldUpdateItemQuantitySuccessfully() throws Exception {
    String listId = performCreateList("My List");
    String itemId = performAddItem(listId, PRODUCT_ID, 2);

    UpdateQuantityRequest updateRequest = UpdateQuantityRequest.builder().quantity(5).build();

    mockMvc
        .perform(
            put("/api/shopping-lists/items/" + itemId + "/quantity")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.listItemId").value(itemId))
        .andExpect(jsonPath("$.quantity").value(5))
        // productName and unit should be unchanged
        .andExpect(jsonPath("$.productName").value("Pomidory"))
        .andExpect(jsonPath("$.unit").value("kg"));
  }

  @Test
  @DisplayName("Should return 404 when updating quantity of non-existent item")
  void shouldReturnNotFoundWhenUpdatingNonExistentItem() throws Exception {
    UpdateQuantityRequest updateRequest = UpdateQuantityRequest.builder().quantity(5).build();

    mockMvc
        .perform(
            put("/api/shopping-lists/items/99999/quantity")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").isArray());
  }

  @Test
  @DisplayName("Should return 400 when updating with zero quantity")
  void shouldReturnBadRequestWhenUpdatingWithZeroQuantity() throws Exception {
    String listId = performCreateList("My List");
    String itemId = performAddItem(listId, PRODUCT_ID, 2);

    UpdateQuantityRequest updateRequest = UpdateQuantityRequest.builder().quantity(0).build();

    mockMvc
        .perform(
            put("/api/shopping-lists/items/" + itemId + "/quantity")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should toggle item checked status from false to true")
  void shouldToggleItemCheckedStatusToTrue() throws Exception {
    String listId = performCreateList("My List");
    String itemId = performAddItem(listId, PRODUCT_ID, 1);

    mockMvc
        .perform(put("/api/shopping-lists/items/" + itemId + "/toggle"))
        .andExpect(status().isNoContent());

    // Verify via get items
    mockMvc
        .perform(get("/api/shopping-lists/" + listId + "/items"))
        .andExpect(jsonPath("$[0].isChecked").value(true));
  }

  @Test
  @DisplayName("Should toggle item checked status back to false")
  void shouldToggleItemCheckedStatusBackToFalse() throws Exception {
    String listId = performCreateList("My List");
    String itemId = performAddItem(listId, PRODUCT_ID, 1);

    // Toggle on
    mockMvc.perform(put("/api/shopping-lists/items/" + itemId + "/toggle"));

    // Toggle off
    mockMvc
        .perform(put("/api/shopping-lists/items/" + itemId + "/toggle"))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/shopping-lists/" + listId + "/items"))
        .andExpect(jsonPath("$[0].isChecked").value(false));
  }

  @Test
  @DisplayName("Should return 404 when toggling non-existent item")
  void shouldReturnNotFoundWhenTogglingNonExistentItem() throws Exception {
    mockMvc.perform(put("/api/shopping-lists/items/99999/toggle")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should remove item from shopping list successfully")
  void shouldRemoveItemFromShoppingListSuccessfully() throws Exception {
    String listId = performCreateList("My List");
    String itemId = performAddItem(listId, PRODUCT_ID, 2);

    assertThat(shoppingListItemRepository.count()).isEqualTo(1);

    mockMvc
        .perform(delete("/api/shopping-lists/items/" + itemId))
        .andExpect(status().isNoContent());

    assertThat(shoppingListItemRepository.count()).isEqualTo(0);

    // List itself should still exist
    mockMvc
        .perform(get("/api/shopping-lists/" + listId + "/items"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @DisplayName("Should return 404 when removing non-existent item")
  void shouldReturnNotFoundWhenRemovingNonExistentItem() throws Exception {
    mockMvc.perform(delete("/api/shopping-lists/items/99999")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should remove only the targeted item leaving others intact")
  void shouldRemoveOnlyTargetedItem() throws Exception {
    String listId = performCreateList("My List");
    String itemId1 = performAddItem(listId, PRODUCT_ID, 2);
    performAddItem(listId, SECOND_PRODUCT_ID, 1);

    assertThat(shoppingListItemRepository.count()).isEqualTo(2);

    mockMvc
        .perform(delete("/api/shopping-lists/items/" + itemId1))
        .andExpect(status().isNoContent());

    assertThat(shoppingListItemRepository.count()).isEqualTo(1);

    mockMvc
        .perform(get("/api/shopping-lists/" + listId + "/items"))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].productName").value("Cebula"));
  }

  // ---- Helpers ---------------------------

  /** Creates a shopping list for TEST_USER and returns its ID as a String. */
  private String performCreateList(String title) throws Exception {
    CreateShoppingListRequest request = CreateShoppingListRequest.builder().title(title).build();

    MvcResult result =
        mockMvc
            .perform(
                post("/api/shopping-lists")
                    .header(AUTH_HEADER, TEST_USER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();

    return objectMapper.readTree(result.getResponse().getContentAsString()).get("listId").asText();
  }

  /**
   * Adds a product to a list and returns the created item ID as a String. Relies on the default
   * mock configured in setUp() for PRODUCT_ID and SECOND_PRODUCT_ID.
   */
  private String performAddItem(String listId, Long productId, int quantity) throws Exception {
    AddProductToListRequest request =
        AddProductToListRequest.builder().productId(productId).quantity(quantity).build();

    MvcResult result =
        mockMvc
            .perform(
                post("/api/shopping-lists/" + listId + "/items")
                    .header(AUTH_HEADER, TEST_USER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();

    return objectMapper
        .readTree(result.getResponse().getContentAsString())
        .get("listItemId")
        .asText();
  }

  /** Builds a fake ProductResponse for use in mock configuration. */
  private ProductResponse buildFakeProduct(Long id, String name, BigDecimal price, String unit) {
    ProductResponse product = new ProductResponse();
    product.setId(id);
    product.setName(name);
    product.setPrice(price);
    product.setUnit(unit);
    return product;
  }
}
