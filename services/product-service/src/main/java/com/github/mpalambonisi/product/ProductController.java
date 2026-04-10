package com.github.mpalambonisi.product;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for product operations. */
@RestController
@RequestMapping("/api/products")
public class ProductController {
  /**
   * Get all products.
   *
   * @return a list of products
   */
  @GetMapping
  public List<Map<String, Object>> getProducts() {
    return List.of(
        Map.of("id", 1, "name", "Pomidory", "price", 5.99),
        Map.of("id", 2, "name", "Cebula", "price", 2.99));
  }
}
