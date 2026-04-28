package com.github.mpalambonisi.shoppinglist.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO for adding a product to a shopping list. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddProductToListRequest {

  @NotNull(message = "Product ID cannot be null.")
  private Long productId;

  @NotNull(message = "Quantity cannot be null.")
  @Min(value = 1, message = "Quantity must be at least 1.")
  private Integer quantity;
}
