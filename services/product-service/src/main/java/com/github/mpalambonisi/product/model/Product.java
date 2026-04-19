package com.github.mpalambonisi.product.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Represents a product available in the system. */
@Entity
@Table(
    name = "products",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "unit", "category_id"})})
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Product {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String name;

  @Column(precision = 10, scale = 2)
  private BigDecimal price;

  @Column(length = 20)
  private String unit; // e.g., "kg", "szt", "opak", "l"

  @Column(length = 1000)
  private String imageUrl;

  @Column(length = 200)
  private String brand;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id", nullable = false)
  @ToString.Exclude
  private Category category;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  /**
   * Constructs a new Product with all necessary details.
   *
   * @param name The name of the product.
   * @param price The current price of the product.
   * @param unit The unit of measure (e.g., kg, szt).
   * @param imageUrl The URL for the product image.
   * @param brand The brand of the product.
   * @param category The category the product belongs to.
   */
  public Product(
      String name,
      BigDecimal price,
      String unit,
      String imageUrl,
      String brand,
      Category category) {
    this.name = name;
    this.price = price;
    this.unit = unit;
    this.imageUrl = imageUrl;
    this.brand = brand;
    this.category = category;
  }
}
