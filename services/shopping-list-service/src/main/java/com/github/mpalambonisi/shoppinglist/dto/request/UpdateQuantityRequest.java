package com.github.mpalambonisi.shoppinglist.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO for updating item quantity. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateQuantityRequest {

  @NotNull(message = "Quantity cannot be null.")
  @Min(value = 1, message = "Quantity must be at least 1.")
  private Integer quantity;
}
