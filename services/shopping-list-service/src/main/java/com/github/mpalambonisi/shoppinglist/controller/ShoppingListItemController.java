package com.github.mpalambonisi.shoppinglist.controller;

import com.github.mpalambonisi.shoppinglist.dto.request.AddProductToListRequest;
import com.github.mpalambonisi.shoppinglist.dto.request.UpdateQuantityRequest;
import com.github.mpalambonisi.shoppinglist.dto.response.ShoppingListItemResponse;
import com.github.mpalambonisi.shoppinglist.model.ShoppingListItem;
import com.github.mpalambonisi.shoppinglist.service.ShoppingListItemService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for managing shopping list items. */
@RestController
@RequestMapping("/api/shopping-lists")
@RequiredArgsConstructor
public class ShoppingListItemController {

  private final ShoppingListItemService shoppingListItemService;

  /**
   * Add a product to a shopping list.
   *
   * @param listId The ID of the shopping list
   * @param request The request containing product ID and quantity
   * @param authentication The authenticated user
   * @return The created shopping list item
   */
  @PostMapping("/{listId}/items")
  public ResponseEntity<ShoppingListItemResponse> addProductToList(
      @PathVariable Long listId, @Valid @RequestBody AddProductToListRequest request) {

    ShoppingListItem item =
        shoppingListItemService.addProductToList(
            listId, request.getProductId(), request.getQuantity());

    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(item));
  }

  /**
   * Get all items in a shopping list.
   *
   * @param listId The ID of the shopping list
   * @param authentication The authenticated user
   * @return List of items in the shopping list
   */
  @GetMapping("/{listId}/items")
  public ResponseEntity<List<ShoppingListItemResponse>> getItemsByListId(
      @PathVariable Long listId) {

    List<ShoppingListItem> items = shoppingListItemService.getItemsByListId(listId);

    List<ShoppingListItemResponse> responses =
        items.stream().map(this::toResponse).collect(Collectors.toList());

    return ResponseEntity.ok(responses);
  }

  /**
   * Update the quantity of an item.
   *
   * @param itemId The ID of the item
   * @param request The request containing the new quantity
   * @param authentication The authenticated user
   * @return The updated item
   */
  @PutMapping("/items/{itemId}/quantity")
  public ResponseEntity<ShoppingListItemResponse> updateQuantity(
      @PathVariable Long itemId, @Valid @RequestBody UpdateQuantityRequest request) {

    ShoppingListItem item = shoppingListItemService.updateQuantity(itemId, request.getQuantity());

    return ResponseEntity.ok(toResponse(item));
  }

  /**
   * Toggle the checked status of an item.
   *
   * @param itemId The ID of the item
   * @param authentication The authenticated user
   * @return No content response
   */
  @PutMapping("/items/{itemId}/toggle")
  public ResponseEntity<Void> toggleItemChecked(@PathVariable Long itemId) {

    shoppingListItemService.toggleItemChecked(itemId);

    return ResponseEntity.noContent().build();
  }

  /**
   * Remove an item from a shopping list.
   *
   * @param itemId The ID of the item to remove
   * @param authentication The authenticated user
   * @return No content response
   */
  @DeleteMapping("/items/{itemId}")
  public ResponseEntity<Void> removeItem(@PathVariable Long itemId) {

    shoppingListItemService.removeItemFromList(itemId);

    return ResponseEntity.noContent().build();
  }

  /**
   * Convert ShoppingListItem entity to response DTO.
   *
   * @param item The entity
   * @return The response DTO
   */
  private ShoppingListItemResponse toResponse(ShoppingListItem item) {
    return ShoppingListItemResponse.builder()
        .listItemId(item.getListItemId())
        .productId(item.getProductId())
        .productName(item.getProductName())
        .quantity(item.getQuantity())
        .unit(item.getUnit())
        .priceAtAddition(item.getPriceAtAddition())
        .isChecked(item.getIsChecked())
        .addedAt(item.getAddedAt())
        .build();
  }
}
