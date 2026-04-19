package com.github.mpalambonisi.product.controller;

import com.github.mpalambonisi.product.dto.CategoryResponse;
import com.github.mpalambonisi.product.model.Category;
import com.github.mpalambonisi.product.service.CategoryService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for managing product categories. */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

  private final CategoryService categoryService;

  /**
   * Get a category by its ID.
   *
   * @param id The category ID
   * @return The category details
   */
  @GetMapping("/{id}")
  public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
    Category category = categoryService.getCategoryById(id);
    return ResponseEntity.ok(toResponse(category));
  }

  /**
   * Get all categories.
   *
   * @return List of all categories
   */
  @GetMapping
  public ResponseEntity<List<CategoryResponse>> getAllCategories() {
    List<Category> categories = categoryService.getAllCategories();
    List<CategoryResponse> responses =
        categories.stream().map(this::toResponse).collect(Collectors.toList());
    return ResponseEntity.ok(responses);
  }

  /**
   * Convert Category entity to response DTO.
   *
   * @param category The category entity
   * @return The response DTO
   */
  private CategoryResponse toResponse(Category category) {
    return CategoryResponse.builder()
        .id(category.getId())
        .name(category.getName())
        .description(category.getDescription())
        .createdAt(category.getCreatedAt())
        .updatedAt(category.getUpdatedAt())
        .build();
  }
}
