package com.github.mpalambonisi.shoppinglist.client;

import com.github.mpalambonisi.common.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * HTTP client for fetching product details from product-service.
 *
 * <p>Called during {@code addProductToList} to snapshot the product's name, unit, and current price
 * onto the {@code ShoppingListItem} at the moment of addition. This means price changes in
 * product-service do NOT retroactively affect existing list items which is the correct behaivour
 */
@Component
public class ProductClient {
  private final WebClient webClient;

  /**
   * Constructs the client with a base URL injected from application properties.
   *
   * @param productServiceUrl base URL of product-service, e.g. {@code http://product-service:8082}
   */
  public ProductClient(
      @Value("${services.product-service.url}") @NonNull String productServiceUrl) {
    this.webClient = WebClient.builder().baseUrl(productServiceUrl).build();
  }

  /**
   * Fetches product details by product ID from product-service.
   *
   * @param productId the ID of the product to fetch
   * @return a {@link ProductResponse} containing name, unit, and price
   * @throws ResourceNotFoundException if product-service returns 404
   * @throws RuntimeException if the call fails for any other reason
   */
  public ProductResponse getProductById(Long productId) {
    try {
      return webClient
          .get()
          .uri("/api/products/{id}", productId)
          .retrieve()
          .bodyToMono(ProductResponse.class)
          .block();
    } catch (WebClientResponseException.NotFound e) {
      throw new ResourceNotFoundException("Product not found with id: " + productId);
    } catch (Exception e) {
      throw new RuntimeException(
          "Failed to fetch product " + productId + " from product-service: " + e.getMessage(), e);
    }
  }
}
