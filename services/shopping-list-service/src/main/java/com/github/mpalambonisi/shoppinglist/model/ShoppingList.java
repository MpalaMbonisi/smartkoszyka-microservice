package com.github.mpalambonisi.shoppinglist.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Represents a user's shopping list, containing multiple items. */
@Entity
@Table(name = "shopping_lists")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class ShoppingList {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "list_id")
  private Long listId;

  @Column(name = "account_email", nullable = false)
  private String accountEmail;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "is_archived", nullable = false)
  private Boolean isArchived = false;

  @OneToMany(mappedBy = "shoppingList", cascade = CascadeType.ALL, orphanRemoval = true)
  @ToString.Exclude
  private List<ShoppingListItem> items = new ArrayList<>();

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
