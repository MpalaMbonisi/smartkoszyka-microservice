package com.github.mpalambonisi.product.service;

import com.github.mpalambonisi.common.exception.ResourceNotFoundException;
import com.github.mpalambonisi.product.model.Product;
import com.github.mpalambonisi.product.repository.ProductRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementation of ProductService for managing products. */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

  private final ProductRepository productRepository;

  @Override
  @Transactional(readOnly = true)
  public Product getProductById(Long id) {

    if (id == null) {
      throw new IllegalArgumentException("Product ID cannot be null");
    }

    return productRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
  }

  @Override
  @Transactional(readOnly = true)
  public List<Product> searchProductsByName(String query) {

    if (query == null || query.isBlank()) {
      throw new IllegalArgumentException("Search query cannot be null or empty");
    }

    return productRepository.findByNameContainingIgnoreCase(query.trim());
  }

  @Override
  @Transactional(readOnly = true)
  public List<Product> getProductsByCategoryId(Long categoryId) {
    if (categoryId == null) {
      throw new IllegalArgumentException("Category ID cannot be null");
    }

    return productRepository.findByCategoryId(categoryId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Product> getAllProducts() {
    return productRepository.findAll();
  }
}
