package com.github.mpalambonisi.shoppinglist.service;

import com.github.mpalambonisi.shoppinglist.model.ShoppingListItem;
import java.util.List;

/** Service interface for managing items within a shopping list. */
public interface ShoppingListItemService {

  ShoppingListItem addProductToList(Long listId, Long productId, Integer quantity);

  ShoppingListItem updateQuantity(Long listItemId, Integer quantity);

  void toggleItemChecked(Long listItemId);

  void removeItemFromList(Long listItemId);

  List<ShoppingListItem> getItemsByListId(Long listId);
}
