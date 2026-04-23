package com.github.mpalambonisi.product.controller;

import com.github.mpalambonisi.product.dto.ProductResponse;
import com.github.mpalambonisi.product.model.Category;
import com.github.mpalambonisi.product.model.Product;
import com.github.mpalambonisi.product.service.ProductService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for managing products. */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

  private final ProductService productService;

  /**
   * Get a product by its ID.
   *
   * @param id The product ID
   * @return The product details
   */
  @GetMapping("/{id}")
  public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
    Product product = productService.getProductById(id);
    return ResponseEntity.ok(toResponse(product));
  }

  /**
   * Search for products by name.
   *
   * @param query The search query
   * @return List of matching products
   */
  @GetMapping("/search")
  public ResponseEntity<List<ProductResponse>> searchProducts(@RequestParam String query) {
    List<Product> products = productService.searchProductsByName(query);
    List<ProductResponse> responses =
        products.stream().map(this::toResponse).collect(Collectors.toList());

    return ResponseEntity.ok(responses);
  }

  /**
   * Get all products in a specific category.
   *
   * @param categoryId The category ID
   * @return List of products in the category
   */
  @GetMapping("/category/{categoryId}")
  public ResponseEntity<List<ProductResponse>> getProductsByCategory(
      @PathVariable Long categoryId) {
    List<Product> products = productService.getProductsByCategoryId(categoryId);
    List<ProductResponse> responses =
        products.stream().map(this::toResponse).collect(Collectors.toList());
    return ResponseEntity.ok(responses);
  }

  /**
   * Get all products.
   *
   * @return List of all products
   */
  @GetMapping
  public ResponseEntity<List<ProductResponse>> getAllProducts() {
    List<Product> products = productService.getAllProducts();
    List<ProductResponse> responses =
        products.stream().map(this::toResponse).collect(Collectors.toList());
    return ResponseEntity.ok(responses);
  }

  private ProductResponse toResponse(Product product) {
    Category category = product.getCategory();
    return ProductResponse.builder()
        .id(product.getId())
        .name(product.getName())
        .price(product.getPrice())
        .unit(product.getUnit())
        .imageUrl(product.getImageUrl())
        .brand(product.getBrand())
        .categoryId(category.getId())
        .categoryName(category.getName())
        .createdAt(product.getCreatedAt())
        .updatedAt(product.getUpdatedAt())
        .build();
  }
}
