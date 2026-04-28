package com.github.mpalambonisi.shoppinglist.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response DTO for shopping list item details. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShoppingListItemResponse {

  private Long listItemId;
  private Long productId;
  private String productName;
  private Integer quantity;
  private String unit;
  private BigDecimal priceAtAddition;
  private Boolean isChecked;
  private LocalDateTime addedAt;
}
