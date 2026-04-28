package com.github.mpalambonisi.shoppinglist.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response DTO for shopping list details. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShoppingListResponse {

  private Long listId;
  private String title;
  private String description;
  private Boolean isArchived;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
