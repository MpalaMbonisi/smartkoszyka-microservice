package com.github.mpalambonisi.shoppinglist.service.impl;

import com.github.mpalambonisi.common.exception.ResourceNotFoundException;
import com.github.mpalambonisi.shoppinglist.client.ProductClient;
import com.github.mpalambonisi.shoppinglist.client.ProductResponse;
import com.github.mpalambonisi.shoppinglist.model.ShoppingList;
import com.github.mpalambonisi.shoppinglist.model.ShoppingListItem;
import com.github.mpalambonisi.shoppinglist.repository.ShoppingListItemRepository;
import com.github.mpalambonisi.shoppinglist.repository.ShoppingListRepository;
import com.github.mpalambonisi.shoppinglist.service.ShoppingListItemService;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementation of ShoppingListItemService for managing items within shopping lists. */
@Service
@RequiredArgsConstructor
public class ShoppingListItemServiceImpl implements ShoppingListItemService {

  private final ShoppingListItemRepository shoppingListItemRepository;
  private final ShoppingListRepository shoppingListRepository;
  private final ProductClient productClient;

  @Override
  @Transactional
  public ShoppingListItem addProductToList(@NonNull Long listId, Long productId, Integer quantity) {
    // Validate quantity
    if (quantity == null || quantity <= 0) {
      throw new IllegalArgumentException("Quantity must be greater than 0");
    }

    // Verify the shopping list exists
    ShoppingList shoppingList =
        shoppingListRepository
            .findById(listId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Shopping list not found with id: " + listId));

    // Prevent duplicate products in the same list
    if (shoppingListItemRepository.existsByShoppingListListIdAndProductId(listId, productId)) {
      throw new IllegalArgumentException("Product is already in this shopping list");
    }

    // Fetch product details from product-service
    // This call will throw ResourceNotFoundException if the product does not exist,
    // which GlobalExceptionHandler maps to HTTP 404 — correct behaviour.
    ProductResponse product = productClient.getProductById(productId);

    // Snapshot product data at the moment of addition
    // If product-service later changes the price, existing list items are unaffected
    ShoppingListItem item = new ShoppingListItem();
    item.setShoppingList(shoppingList);
    item.setProductId(product.getId());
    item.setProductName(product.getName());
    item.setUnit(product.getUnit());
    item.setPriceAtAddition(product.getPrice());
    item.setQuantity(quantity);
    item.setIsChecked(false);

    return shoppingListItemRepository.save(item);
  }

  @Override
  @Transactional
  public ShoppingListItem updateQuantity(Long listItemId, Integer quantity) {

    if (quantity == null || quantity <= 0) {
      throw new IllegalArgumentException("Quantity must be greater than 0");
    }

    // Null guard fixes the @NonNull warning — findById requires a non-null argument.
    // In practice listItemId arrives from a @PathVariable so it won't be null at runtime,
    // but the guard makes the null-analysis tool happy and prevents any edge-case NPE.
    if (listItemId == null) {
      throw new IllegalArgumentException("List item ID cannot be null");
    }

    ShoppingListItem item =
        shoppingListItemRepository
            .findById(listItemId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Shopping list item not found with id: " + listItemId));

    item.setQuantity(quantity);

    return shoppingListItemRepository.save(item);
  }

  @Override
  @Transactional
  public void toggleItemChecked(Long listItemId) {

    if (listItemId == null) {
      throw new IllegalArgumentException("List item ID cannot be null");
    }

    ShoppingListItem item =
        shoppingListItemRepository
            .findById(listItemId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Shopping list item not found with id: " + listItemId));

    item.setIsChecked(!item.getIsChecked());

    shoppingListItemRepository.save(item);
  }

  @Override
  @Transactional
  public void removeItemFromList(Long listItemId) {

    if (listItemId == null) {
      throw new IllegalArgumentException("List item ID cannot be null");
    }

    ShoppingListItem item =
        shoppingListItemRepository
            .findById(listItemId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Shopping list item not found with id: " + listItemId));

    shoppingListItemRepository.delete(item);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ShoppingListItem> getItemsByListId(Long listId) {
    return shoppingListItemRepository.findByShoppingListListId(listId);
  }
}
