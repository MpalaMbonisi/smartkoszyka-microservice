package com.github.mpalambonisi.product.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.mpalambonisi.common.exception.ResourceNotFoundException;
import com.github.mpalambonisi.product.model.Category;
import com.github.mpalambonisi.product.model.Product;
import com.github.mpalambonisi.product.repository.ProductRepository;
import com.github.mpalambonisi.product.service.ProductServiceImpl;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
public class ProductServiceImplTest {

  @Mock private ProductRepository productRepository;

  @InjectMocks private ProductServiceImpl productService;

  private Product testProduct;
  private Category testCategory;
  private Long productId;

  @BeforeEach
  void setUp() {
    productId = 1L;

    testCategory = new Category("Warzywa", "Fresh vegetables");
    testCategory.setId(1L);

    testProduct = new Product();
    testProduct.setId(productId);
    testProduct.setName("Tomatoes");
    testProduct.setPrice(new BigDecimal("5.99"));
    testProduct.setUnit("kg");
    testProduct.setImageUrl("http://example.com/tomato.jpg");
    testProduct.setBrand("Fresh Farm");
    testProduct.setCategory(testCategory);
  }

  @Test
  @DisplayName("Should get product by ID successfully")
  void shouldGetProductByIdSuccessfully() {
    // Given
    when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));

    // When
    Product result = productService.getProductById(productId);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(productId);
    assertThat(result.getName()).isEqualTo("Tomatoes");
    assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("5.99"));
    verify(productRepository).findById(productId);
  }

  @Test
  @DisplayName("Should throw exception when product ID is null")
  void shouldThrowExceptionWhenProductIdIsNull() {
    // When & Then
    assertThatThrownBy(() -> productService.getProductById(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Product ID cannot be null");
  }

  @Test
  @DisplayName("Should throw exception when product not found by ID")
  void shouldThrowExceptionWhenProductNotFoundById() {
    // Given
    when(productRepository.findById(productId)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> productService.getProductById(productId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Product not found with id: 1");

    verify(productRepository).findById(productId);
  }

  @Test
  @DisplayName("Should search products by name successfully")
  void shouldSearchProductsByNameSuccessfully() {
    // Given
    String query = "tomato";
    Product product2 = new Product();
    product2.setId(2L);
    product2.setName("Cherry Tomatoes");
    product2.setCategory(testCategory);

    List<Product> products = Arrays.asList(testProduct, product2);
    when(productRepository.findByNameContainingIgnoreCase(query)).thenReturn(products);

    // When
    List<Product> result = productService.searchProductsByName(query);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result).contains(testProduct, product2);
    verify(productRepository).findByNameContainingIgnoreCase(query);
  }

  @Test
  @DisplayName("Should trim search query")
  void shouldTrimSearchQuery() {
    // Given
    String query = "  tomato  ";
    when(productRepository.findByNameContainingIgnoreCase("tomato"))
        .thenReturn(List.of(testProduct));

    // When
    productService.searchProductsByName(query);

    // Then
    verify(productRepository).findByNameContainingIgnoreCase("tomato");
  }

  @Test
  @DisplayName("Should return empty list when no products match search")
  void shouldReturnEmptyListWhenNoProductsMatchSearch() {
    // Given
    String query = "nonexistent";
    when(productRepository.findByNameContainingIgnoreCase(query)).thenReturn(List.of());

    // When
    List<Product> result = productService.searchProductsByName(query);

    // Then
    assertThat(result).isEmpty();
    verify(productRepository).findByNameContainingIgnoreCase(query);
  }

  @Test
  @DisplayName("Should throw exception when search query is null")
  void shouldThrowExceptionWhenSearchQueryIsNull() {
    // When & Then
    assertThatThrownBy(() -> productService.searchProductsByName(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Search query cannot be null or empty");
  }

  @Test
  @DisplayName("Should throw exception when search query is empty")
  void shouldThrowExceptionWhenSearchQueryIsEmpty() {
    // When & Then
    assertThatThrownBy(() -> productService.searchProductsByName("  "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Search query cannot be null or empty");
  }

  @Test
  @DisplayName("Should get products by category ID successfully")
  void shouldGetProductsByCategoryIdSuccessfully() {
    // Given
    Long categoryId = 1L;
    Product product2 = new Product();
    product2.setId(2L);
    product2.setName("Cucumbers");
    product2.setCategory(testCategory);

    List<Product> products = Arrays.asList(testProduct, product2);
    when(productRepository.findByCategoryId(categoryId)).thenReturn(products);

    // When
    List<Product> result = productService.getProductsByCategoryId(categoryId);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result).allMatch(p -> p.getCategory().getId().equals(categoryId));
    verify(productRepository).findByCategoryId(categoryId);
  }

  @Test
  @DisplayName("Should return empty list when no products in category")
  void shouldReturnEmptyListWhenNoProductsInCategory() {
    // Given
    Long categoryId = 99L;
    when(productRepository.findByCategoryId(categoryId)).thenReturn(List.of());

    // When
    List<Product> result = productService.getProductsByCategoryId(categoryId);

    // Then
    assertThat(result).isEmpty();
    verify(productRepository).findByCategoryId(categoryId);
  }

  @Test
  @DisplayName("Should throw exception when category ID is null")
  void shouldThrowExceptionWhenCategoryIdIsNull() {
    // When & Then
    assertThatThrownBy(() -> productService.getProductsByCategoryId(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Category ID cannot be null");
  }

  @Test
  @DisplayName("Should get all products successfully")
  void shouldGetAllProductsSuccessfully() {
    // Given
    Product product2 = new Product();
    product2.setId(2L);
    product2.setName("Cucumbers");

    List<Product> products = Arrays.asList(testProduct, product2);
    when(productRepository.findAll()).thenReturn(products);

    // When
    List<Product> result = productService.getAllProducts();

    // Then
    assertThat(result).hasSize(2);
    assertThat(result).contains(testProduct, product2);
    verify(productRepository).findAll();
  }

  @Test
  @DisplayName("Should return empty list when no products exist")
  void shouldReturnEmptyListWhenNoProductsExist() {
    // Given
    when(productRepository.findAll()).thenReturn(List.of());

    // When
    List<Product> result = productService.getAllProducts();

    // Then
    assertThat(result).isEmpty();
    verify(productRepository).findAll();
  }
}
