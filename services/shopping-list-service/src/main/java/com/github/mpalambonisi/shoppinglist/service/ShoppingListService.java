package com.github.mpalambonisi.shoppinglist.service;

import com.github.mpalambonisi.shoppinglist.model.ShoppingList;
import java.util.List;

/** Service interface for managing user shopping lists. */
public interface ShoppingListService {

  ShoppingList createShoppingList(String accountEmail, String title, String description);

  ShoppingList getShoppingListById(Long listId);

  List<ShoppingList> getActiveShoppingListByAccountEmail(String accountEmail);

  List<ShoppingList> getAllShoppingListsByAccountEmail(String accountEmail);

  ShoppingList updateShoppingListTitle(Long listId, String title);

  void archiveShoppingList(Long listId);

  void deleteShoppingList(Long listId);
}
