package com.github.mpalambonisi.product.service;

import com.github.mpalambonisi.common.exception.ResourceNotFoundException;
import com.github.mpalambonisi.product.model.Category;
import com.github.mpalambonisi.product.repository.CategoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementation of CategoryService for managing categories. */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

  private final CategoryRepository categoryRepository;

  @Override
  @Transactional(readOnly = true)
  public Category getCategoryById(Long id) {

    if (id == null) {
      throw new IllegalArgumentException("Category ID cannot be null");
    }

    return categoryRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
  }

  @Override
  @Transactional(readOnly = true)
  public List<Category> getAllCategories() {
    return categoryRepository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public Category getCategoryByName(String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Category name cannot be null or empty");
    }

    return categoryRepository
        .findByName(name.trim())
        .orElseThrow(
            () -> new ResourceNotFoundException("Category not found with name: " + name.trim()));
  }
}
