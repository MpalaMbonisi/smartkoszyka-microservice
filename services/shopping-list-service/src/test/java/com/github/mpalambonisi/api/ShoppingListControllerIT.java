package com.github.mpalambonisi.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mpalambonisi.shoppinglist.ShoppingListApplication;
import com.github.mpalambonisi.shoppinglist.client.ProductClient;
import com.github.mpalambonisi.shoppinglist.dto.request.CreateShoppingListRequest;
import com.github.mpalambonisi.shoppinglist.dto.request.UpdateShoppingListRequest;
import com.github.mpalambonisi.shoppinglist.repository.ShoppingListRepository;
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
 * Integration tests for ShoppingListController.
 *
 * <p>Key design decisions:
 *
 * <ul>
 *   <li>{@code @MockBean ProductClient} — prevents the real ProductClient from trying to connect to
 *       product-service during context startup and during tests that don't exercise item addition.
 *       The ShoppingListController tests don't call addProductToList, so the mock never needs to
 *       return anything here.
 *   <li>Every request that hits an accountEmail-dependent endpoint sends the {@code
 *       X-Authenticated-User} header — the same header the API gateway stamps on requests after JWT
 *       validation. In production this comes from the gateway; in tests we send it directly because
 *       we are testing the service layer, not the gateway.
 *   <li>{@code @BeforeEach} clears the repository so tests are fully isolated. Without this, count
 *       assertions like {@code $.length() == 2} fail when tests run in any order other than the one
 *       you wrote them in.
 *   <li>The "unauthorized when no header" test replaces the original "unauthorized when no JWT"
 *       test. This service never issues 401 — that is the gateway's job. The correct boundary test
 *       here is that a missing header causes a 400 Bad Request from Spring's {@code @RequestHeader}
 *       binding.
 * </ul>
 */
@SpringBootTest(classes = ShoppingListApplication.class)
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Shopping List Controller Integration Tests")
public class ShoppingListControllerIT {

  // The gateway header this service uses instead of a JWT
  private static final String AUTH_HEADER = "X-Authenticated-User";
  private static final String TEST_USER = "nicole.smith@example.com";

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
    // Provide a dummy URL so ProductClient's @Value injection succeeds at context startup.
    // The bean itself is mocked below so this URL is never actually called.
    registry.add("services.product-service.url", () -> "http://localhost:9999");
  }

  // Replaces the real ProductClient bean in the Spring context with a Mockito mock.
  // This means no real HTTP calls are attempted during these tests.
  // Tests for ShoppingListItemControllerIT (item addition) will configure this mock
  // to return fake product data via Mockito.when(...).thenReturn(...).
  @MockBean private ProductClient productClient;

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private ShoppingListRepository shoppingListRepository;

  @BeforeEach
  void setUp() {
    shoppingListRepository.deleteAll();
  }

  @Test
  @DisplayName("Should create shopping list successfully")
  void shouldCreateShoppingListSuccessfully() throws Exception {
    CreateShoppingListRequest request =
        CreateShoppingListRequest.builder()
            .title("Weekly Shopping")
            .description("Groceries for the week")
            .build();

    mockMvc
        .perform(
            post("/api/shopping-lists")
                .header(AUTH_HEADER, TEST_USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.listId").exists())
        .andExpect(jsonPath("$.title").value("Weekly Shopping"))
        .andExpect(jsonPath("$.description").value("Groceries for the week"))
        .andExpect(jsonPath("$.isArchived").value(false))
        .andExpect(jsonPath("$.createdAt").exists());

    assertThat(shoppingListRepository.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should return 400 when X-Authenticated-User header is missing")
  void shouldReturnBadRequestWhenAuthHeaderIsMissing() throws Exception {
    // This service does not issue 401 — that is the gateway's responsibility.
    // A missing header means @RequestHeader binding fails -> Spring returns 400.
    CreateShoppingListRequest request =
        CreateShoppingListRequest.builder().title("Test List").build();

    mockMvc
        .perform(
            post("/api/shopping-lists")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return bad request when title is missing")
  void shouldReturnBadRequestWhenTitleIsMissing() throws Exception {
    CreateShoppingListRequest request =
        CreateShoppingListRequest.builder().title("").description("Test").build();

    mockMvc
        .perform(
            post("/api/shopping-lists")
                .header(AUTH_HEADER, TEST_USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").isArray());
  }

  @Test
  @DisplayName("Should get active shopping lists")
  void shouldGetActiveShoppingLists() throws Exception {
    // Create two lists
    performCreate("List 1");
    performCreate("List 2");

    mockMvc
        .perform(get("/api/shopping-lists/active").header(AUTH_HEADER, TEST_USER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].title").exists())
        .andExpect(jsonPath("$[1].title").exists());
  }

  @Test
  @DisplayName("Should get shopping list by ID")
  void shouldGetShoppingListById() throws Exception {
    String listId = performCreate("Test List");

    mockMvc
        .perform(get("/api/shopping-lists/" + listId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.listId").value(listId))
        .andExpect(jsonPath("$.title").value("Test List"));
  }

  @Test
  @DisplayName("Should return not found for non-existent list")
  void shouldReturnNotFoundForNonExistentList() throws Exception {
    mockMvc
        .perform(get("/api/shopping-lists/9999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").isArray());
  }

  @Test
  @DisplayName("Should update shopping list title")
  void shouldUpdateShoppingListTitle() throws Exception {
    String listId = performCreate("Old Title");

    UpdateShoppingListRequest updateRequest =
        UpdateShoppingListRequest.builder().title("New Title").build();

    mockMvc
        .perform(
            put("/api/shopping-lists/" + listId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("New Title"));
  }

  @Test
  @DisplayName("Should archive shopping list")
  void shouldArchiveShoppingList() throws Exception {
    String listId = performCreate("To Archive");

    mockMvc
        .perform(put("/api/shopping-lists/" + listId + "/archive"))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/shopping-lists/" + listId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isArchived").value(true));
  }

  @Test
  @DisplayName("Should delete shopping list")
  void shouldDeleteShoppingList() throws Exception {
    String listId = performCreate("To Delete");

    mockMvc.perform(delete("/api/shopping-lists/" + listId)).andExpect(status().isNoContent());

    mockMvc.perform(get("/api/shopping-lists/" + listId)).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should get all shopping lists including archived")
  void shouldGetAllShoppingListsIncludingArchived() throws Exception {
    String listId = performCreate("List to Archive");

    mockMvc.perform(
        put("/api/shopping-lists/" + listId + "/archive").header(AUTH_HEADER, TEST_USER));

    mockMvc
        .perform(get("/api/shopping-lists/all").header(AUTH_HEADER, TEST_USER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].isArchived").value(true));
  }

  @Test
  @DisplayName("Should only return lists belonging to the authenticated user")
  void shouldOnlyReturnListsBelongingToAuthenticatedUser() throws Exception {
    // Create a list as TEST_USER
    performCreate("My List");

    // Create a list as a different user
    CreateShoppingListRequest otherRequest =
        CreateShoppingListRequest.builder().title("Other User List").build();
    mockMvc.perform(
        post("/api/shopping-lists")
            .header(AUTH_HEADER, "other.user@example.com")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(otherRequest)));

    // TEST_USER should only see their own list
    mockMvc
        .perform(get("/api/shopping-lists/active").header(AUTH_HEADER, TEST_USER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].title").value("My List"));
  }

  // ── Helper ────────────────────────────────────────────────────────────────

  /**
   * Creates a shopping list for TEST_USER and returns its ID as a String. Extracted to avoid
   * duplicating the create-and-extract-id pattern in every test.
   */
  private String performCreate(String title) throws Exception {
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
}
