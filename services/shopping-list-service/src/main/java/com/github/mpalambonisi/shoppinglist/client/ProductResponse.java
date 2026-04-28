package com.github.mpalambonisi.shoppinglist.client;

import java.math.BigDecimal;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for deserialising product details returned by product-service
 *
 * <p>Only the fields this service actually needs are declared — Jackson ignores the rest.
 */
@Data
@NoArgsConstructor
public class ProductResponse {

  private Long id;
  private String name;
  private BigDecimal price;
  private String unit;
}
