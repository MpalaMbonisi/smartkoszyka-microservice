package com.github.mpalambonisi.product.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.mpalambonisi.product.model.Category;
import com.github.mpalambonisi.product.model.Product;
import com.github.mpalambonisi.product.repository.CategoryRepository;
import com.github.mpalambonisi.product.repository.ProductRepository;
import java.math.BigDecimal;
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
@DisplayName("Product Controller Integration Tests")
public class ProductControllerIT {

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
  @Autowired private ProductRepository productRepository;
  @Autowired private CategoryRepository categoryRepository;

  private Long productId;
  private Long categoryId;

  @BeforeEach
  void setUp() {
    // Delete in correct FK order — products reference categories
    productRepository.deleteAll();
    categoryRepository.deleteAll();

    Category category = new Category("Warzywa", "Świeże warzywa");
    category = categoryRepository.save(category);
    categoryId = category.getId();

    Product product1 =
        new Product(
            "Pomidory",
            new BigDecimal("5.99"),
            "kg",
            "http://biedronka.pl/pomidory-01.jpg",
            "Biedronka",
            category);
    product1 = productRepository.save(product1);
    productId = product1.getId();

    productRepository.save(
        new Product(
            "Pomidory Koktajlowe",
            new BigDecimal("7.99"),
            "kg",
            "http://biedronka.pl/pomidory-02.jpg",
            "Biedronka",
            category));

    productRepository.save(
        new Product(
            "Cebula Luz",
            new BigDecimal("3.99"),
            "kg",
            "http://biedronka.pl/cebula-01.jpg",
            "Biedronka",
            category));
  }

  @Test
  @DisplayName("Should get product by ID successfully")
  void shouldGetProductByIdSuccessfully() throws Exception {
    mockMvc
        .perform(get("/api/products/" + productId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(productId))
        .andExpect(jsonPath("$.name").value("Pomidory"))
        .andExpect(jsonPath("$.price").value(5.99))
        .andExpect(jsonPath("$.unit").value("kg"))
        .andExpect(jsonPath("$.brand").value("Biedronka"))
        .andExpect(jsonPath("$.categoryId").value(categoryId))
        .andExpect(jsonPath("$.categoryName").value("Warzywa"))
        .andExpect(jsonPath("$.imageUrl").value("http://biedronka.pl/pomidory-01.jpg"))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.updatedAt").exists());
  }

  @Test
  @DisplayName("Should return not found for non-existent product")
  void shouldReturnNotFoundForNonExistentProduct() throws Exception {
    mockMvc
        .perform(get("/api/products/99999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").isArray());
  }

  @Test
  @DisplayName("Should search products by name successfully")
  void shouldSearchProductsByNameSuccessfully() throws Exception {
    mockMvc
        .perform(get("/api/products/search?query=pomidory"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].name").value("Pomidory"))
        .andExpect(jsonPath("$[1].name").value("Pomidory Koktajlowe"));
  }

  @Test
  @DisplayName("Should search products case-insensitively")
  void shouldSearchProductsCaseInsensitively() throws Exception {
    mockMvc
        .perform(get("/api/products/search?query=POMIDORY"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  @DisplayName("Should return empty list when no products match search")
  void shouldReturnEmptyListWhenNoProductsMatchSearch() throws Exception {
    mockMvc
        .perform(get("/api/products/search?query=nonexistent"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @DisplayName("Should return bad request when search query is blank")
  void shouldReturnBadRequestWhenSearchQueryIsBlank() throws Exception {
    mockMvc.perform(get("/api/products/search?query=  ")).andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should get products by category ID successfully")
  void shouldGetProductsByCategoryIdSuccessfully() throws Exception {
    mockMvc
        .perform(get("/api/products/category/" + categoryId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].categoryId").value(categoryId))
        .andExpect(jsonPath("$[1].categoryId").value(categoryId))
        .andExpect(jsonPath("$[2].categoryId").value(categoryId));
  }

  @Test
  @DisplayName("Should return empty list when no products in category")
  void shouldReturnEmptyListWhenNoProductsInCategory() throws Exception {
    mockMvc
        .perform(get("/api/products/category/99999"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @DisplayName("Should get all products successfully")
  void shouldGetAllProductsSuccessfully() throws Exception {
    mockMvc
        .perform(get("/api/products"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(3));
  }

  /*
   * NOTE: Auth/JWT tests are intentionally absent here.
   * See CategoryControllerIT for the architectural explanation.
   */
}
