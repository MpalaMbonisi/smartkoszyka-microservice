package com.github.mpalambonisi.product.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response DTO for category details. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryResponse {

  private Long id;
  private String name;
  private String description;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
