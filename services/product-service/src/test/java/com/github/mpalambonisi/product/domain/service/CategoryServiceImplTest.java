package com.github.mpalambonisi.product.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.mpalambonisi.common.exception.ResourceNotFoundException;
import com.github.mpalambonisi.product.model.Category;
import com.github.mpalambonisi.product.repository.CategoryRepository;
import com.github.mpalambonisi.product.service.CategoryServiceImpl;
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
@DisplayName("CategoryService Unit Tests")
public class CategoryServiceImplTest {

  @Mock private CategoryRepository categoryRepository;

  @InjectMocks private CategoryServiceImpl categoryService;

  private Category testCategory;
  private Long categoryId;

  @BeforeEach
  void setUp() {
    categoryId = 1L;

    testCategory = new Category("Warzywa", "Fresh vegetables");
    testCategory.setId(categoryId);
  }

  @Test
  @DisplayName("Should get category by ID successfully")
  void shouldGetCategoryByIdSuccessfully() {
    // Given
    when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));

    // When
    Category result = categoryService.getCategoryById(categoryId);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(categoryId);
    assertThat(result.getName()).isEqualTo("Warzywa");
    assertThat(result.getDescription()).isEqualTo("Fresh vegetables");
    verify(categoryRepository).findById(categoryId);
  }

  @Test
  @DisplayName("Should throw exception when category ID is null")
  void shouldThrowExceptionWhenCategoryIdIsNull() {
    // When & Then
    assertThatThrownBy(() -> categoryService.getCategoryById(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Category ID cannot be null");
  }

  @Test
  @DisplayName("Should throw exception when category not found by ID")
  void shouldThrowExceptionWhenCategoryNotFoundById() {
    // Given
    when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> categoryService.getCategoryById(categoryId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Category not found with id: 1");

    verify(categoryRepository).findById(categoryId);
  }

  @Test
  @DisplayName("Should get all categories successfully")
  void shouldGetAllCategoriesSuccessfully() {
    // Given
    Category category2 = new Category("Owoce", "Fresh fruits");
    category2.setId(2L);

    List<Category> categories = Arrays.asList(testCategory, category2);
    when(categoryRepository.findAll()).thenReturn(categories);

    // When
    List<Category> result = categoryService.getAllCategories();

    // Then
    assertThat(result).hasSize(2);
    assertThat(result).contains(testCategory, category2);
    verify(categoryRepository).findAll();
  }

  @Test
  @DisplayName("Should return empty list when no categories exist")
  void shouldReturnEmptyListWhenNoCategoriesExist() {
    // Given
    when(categoryRepository.findAll()).thenReturn(List.of());

    // When
    List<Category> result = categoryService.getAllCategories();

    // Then
    assertThat(result).isEmpty();
    verify(categoryRepository).findAll();
  }

  @Test
  @DisplayName("Should get category by name successfully")
  void shouldGetCategoryByNameSuccessfully() {
    // Given
    String categoryName = "Warzywa";
    when(categoryRepository.findByName(categoryName)).thenReturn(Optional.of(testCategory));

    // When
    Category result = categoryService.getCategoryByName(categoryName);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo(categoryName);
    verify(categoryRepository).findByName(categoryName);
  }

  @Test
  @DisplayName("Should trim category name when searching")
  void shouldTrimCategoryNameWhenSearching() {
    // Given
    String categoryName = "  Warzywa  ";
    when(categoryRepository.findByName("Warzywa")).thenReturn(Optional.of(testCategory));

    // When
    Category result = categoryService.getCategoryByName(categoryName);

    // Then
    assertThat(result).isNotNull();
    verify(categoryRepository).findByName("Warzywa");
  }

  @Test
  @DisplayName("Should throw exception when category not found by name")
  void shouldThrowExceptionWhenCategoryNotFoundByName() {
    // Given
    String categoryName = "NonExistent";
    when(categoryRepository.findByName(categoryName)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> categoryService.getCategoryByName(categoryName))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Category not found with name: NonExistent");

    verify(categoryRepository).findByName(categoryName);
  }

  @Test
  @DisplayName("Should throw exception when category name is null")
  void shouldThrowExceptionWhenCategoryNameIsNull() {
    // When & Then
    assertThatThrownBy(() -> categoryService.getCategoryByName(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Category name cannot be null or empty");
  }

  @Test
  @DisplayName("Should throw exception when category name is empty")
  void shouldThrowExceptionWhenCategoryNameIsEmpty() {
    // When & Then
    assertThatThrownBy(() -> categoryService.getCategoryByName("  "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Category name cannot be null or empty");
  }
}
