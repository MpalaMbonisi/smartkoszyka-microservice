package com.github.mpalambonisi.shoppinglist.service.impl;

import com.github.mpalambonisi.common.exception.ResourceNotFoundException;
import com.github.mpalambonisi.shoppinglist.model.ShoppingList;
import com.github.mpalambonisi.shoppinglist.repository.ShoppingListRepository;
import com.github.mpalambonisi.shoppinglist.service.ShoppingListService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementation of ShoppingListService for managing user shopping lists. */
@Service
@RequiredArgsConstructor
public class ShoppingListServiceImpl implements ShoppingListService {

  private final ShoppingListRepository shoppingListRepository;

  @Override
  @Transactional
  public ShoppingList createShoppingList(String accountEmail, String title, String description) {
    // Validate title
    if (title == null || title.trim().isEmpty()) {
      throw new IllegalArgumentException("Title cannot be null or empty");
    }

    // Create shopping list
    ShoppingList shoppingList = new ShoppingList();
    shoppingList.setAccountEmail(accountEmail);
    shoppingList.setTitle(title);
    shoppingList.setDescription(description);
    shoppingList.setIsArchived(false);

    return shoppingListRepository.save(shoppingList);
  }

  @Override
  @Transactional(readOnly = true)
  public ShoppingList getShoppingListById(Long listId) {

    if (listId == null) {
      throw new IllegalArgumentException("List item ID cannot be null");
    }

    return shoppingListRepository
        .findById(listId)
        .orElseThrow(
            () -> new ResourceNotFoundException("Shopping list not found with id: " + listId));
  }

  @Override
  @Transactional(readOnly = true)
  public List<ShoppingList> getActiveShoppingListByAccountEmail(String accountEmail) {
    return shoppingListRepository.findByAccountEmailAndIsArchivedFalse(accountEmail);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ShoppingList> getAllShoppingListsByAccountEmail(String accountEmail) {
    return shoppingListRepository.findByAccountEmail(accountEmail);
  }

  @Override
  @Transactional
  public ShoppingList updateShoppingListTitle(Long listId, String title) {
    // Validate title
    if (title == null || title.trim().isEmpty()) {
      throw new IllegalArgumentException("Title cannot be null or empty");
    }

    if (listId == null) {
      throw new IllegalArgumentException("List item ID cannot be null");
    }

    // Find shopping list
    ShoppingList shoppingList =
        shoppingListRepository
            .findById(listId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Shopping list not found with id: " + listId));

    // Update title
    shoppingList.setTitle(title);

    return shoppingListRepository.save(shoppingList);
  }

  @Override
  @Transactional
  public void archiveShoppingList(Long listId) {

    if (listId == null) {
      throw new IllegalArgumentException("List item ID cannot be null");
    }

    // Find shopping list
    ShoppingList shoppingList =
        shoppingListRepository
            .findById(listId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Shopping list not found with id: " + listId));

    // Archive it
    shoppingList.setIsArchived(true);

    shoppingListRepository.save(shoppingList);
  }

  @Override
  @Transactional
  public void deleteShoppingList(Long listId) {
    if (listId == null) {
      throw new IllegalArgumentException("List item ID cannot be null");
    }

    // Find shopping list
    ShoppingList shoppingList =
        shoppingListRepository
            .findById(listId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Shopping list not found with id: " + listId));

    // Delete it
    shoppingListRepository.delete(shoppingList);
  }
}
