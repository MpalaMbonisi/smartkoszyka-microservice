package com.github.mpalambonisi.shoppinglist.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO for updating a shopping list. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateShoppingListRequest {

  @NotEmpty(message = "Title cannot be empty.")
  @NotBlank(message = "Title cannot be blank.")
  private String title;
}
