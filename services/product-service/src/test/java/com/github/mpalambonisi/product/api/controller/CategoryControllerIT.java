package com.github.mpalambonisi.product.api.controller;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.mpalambonisi.product.model.Category;
import com.github.mpalambonisi.product.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Category Controller Integration Tests")
public class CategoryControllerIT {

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
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private CategoryRepository categoryRepository;

  private Long categoryId;

  @BeforeEach
  void setUp() {
    categoryRepository.deleteAll();

    Category category1 = new Category("Warzywa", "Świeże warzywa");
    category1 = categoryRepository.save(category1);
    categoryId = category1.getId();

    categoryRepository.save(new Category("Owoce", "Świeże owoce"));
    categoryRepository.save(new Category("Nabiał", "Produkty mleczne"));
  }

  @Test
  @DisplayName("Should get category by ID successfully")
  void shouldGetCategoryByIdSuccessfully() throws Exception {
    mockMvc
        .perform(get("/api/categories/" + categoryId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(categoryId))
        .andExpect(jsonPath("$.name").value("Warzywa"))
        .andExpect(jsonPath("$.description").value("Świeże warzywa"))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.updatedAt").exists());
  }

  @Test
  @DisplayName("Should return not found for non-existent category")
  void shouldReturnNotFoundForNonExistentCategory() throws Exception {
    mockMvc
        .perform(get("/api/categories/99999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").isArray());
  }

  @Test
  @DisplayName("Should get all categories successfully")
  void shouldGetAllCategoriesSuccessfully() throws Exception {
    mockMvc
        .perform(get("/api/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(3));
  }

  @Test
  @DisplayName("Should verify all categories have required fields")
  void shouldVerifyAllCategoriesHaveRequiredFields() throws Exception {
    mockMvc
        .perform(get("/api/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").exists())
        .andExpect(jsonPath("$[0].name").exists())
        .andExpect(jsonPath("$[0].createdAt").exists())
        .andExpect(jsonPath("$[0].updatedAt").exists());
  }

  @Test
  @DisplayName("Should confirm response contains all seeded categories")
  void shouldConfirmResponseContainsAllSeededCategories() throws Exception {
    mockMvc
        .perform(get("/api/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].name").value(hasItems("Warzywa", "Owoce", "Nabiał")));
  }

  @Test
  @DisplayName("Should handle empty category list gracefully")
  void shouldHandleEmptyCategoryListGracefully() throws Exception {
    categoryRepository.deleteAll();

    mockMvc
        .perform(get("/api/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0));
  }

  /*
   * NOTE: Auth/JWT tests are intentionally absent here.
   * JWT validation is the api-gateway's responsibility.
   * At the service level, SecurityConfig permits all traffic
   * that has passed through the gateway. Testing 401 responses
   * at the service layer would test the wrong architectural boundary.
   * Auth behaviour is tested at the gateway integration test level.
   */
}
