package com.github.mpalambonisi.product.service;

import com.github.mpalambonisi.product.model.Category;
import java.util.List;

/** Service interface for managing categories. */
public interface CategoryService {

  Category getCategoryById(Long id);

  List<Category> getAllCategories();

  Category getCategoryByName(String name);
}
