package com.github.mpalambonisi.product.service;

import com.github.mpalambonisi.product.model.Product;
import java.util.List;

/** Service interface for managing products. */
public interface ProductService {

  Product getProductById(Long id);

  List<Product> searchProductsByName(String query);

  List<Product> getProductsByCategoryId(Long categoryId);

  List<Product> getAllProducts();
}
