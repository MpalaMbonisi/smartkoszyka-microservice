package com.github.mpalambonisi.product.repository;

import com.github.mpalambonisi.product.model.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository for managing Product entities. */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
  List<Product> findByNameContainingIgnoreCase(String name);

  List<Product> findByCategoryId(Long categoryId);

  /**
   * Find a product by name, unit, and category ID (case-insensitive).
   *
   * @param name The product name
   * @param unit The product unit
   * @param categoryId The category ID
   * @return Optional containing the product if found
   */
  @Query(
      "SELECT p FROM Product p WHERE LOWER(p.name) = LOWER(:name) "
          + "AND LOWER(p.unit) = LOWER(:unit) AND p.category.id = :categoryId")
  Optional<Product> findByNameAndUnitAndCategoryId(
      @Param("name") String name, @Param("unit") String unit, @Param("categoryId") Long categoryId);
}
