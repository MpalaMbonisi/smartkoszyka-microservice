package com.github.mpalambonisi.shoppinglist.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

/** Represents an individual item within a shopping list. */
@Entity
@Table(name = "shopping_list_items")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class ShoppingListItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "list_item_id")
  private Long listItemId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "list_id", nullable = false)
  @ToString.Exclude
  private ShoppingList shoppingList;

  @Column(nullable = false)
  private Long productId;

  @Column(nullable = false)
  private String productName;

  @Column(nullable = false)
  private Integer quantity;

  @Column(length = 20)
  private String unit;

  @Column(name = "price_at_addition", precision = 10, scale = 2)
  private BigDecimal priceAtAddition;

  @Column(name = "is_checked", nullable = false)
  private Boolean isChecked = false;

  @CreationTimestamp
  @Column(name = "added_at", nullable = false, updatable = false)
  private LocalDateTime addedAt;
}
