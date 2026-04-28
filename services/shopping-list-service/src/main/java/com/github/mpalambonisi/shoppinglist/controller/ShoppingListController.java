package com.github.mpalambonisi.shoppinglist.controller;

import com.github.mpalambonisi.shoppinglist.dto.request.CreateShoppingListRequest;
import com.github.mpalambonisi.shoppinglist.dto.request.UpdateShoppingListRequest;
import com.github.mpalambonisi.shoppinglist.dto.response.ShoppingListResponse;
import com.github.mpalambonisi.shoppinglist.model.ShoppingList;
import com.github.mpalambonisi.shoppinglist.service.ShoppingListService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for managing shopping lists. */
@RestController
@RequestMapping("/api/shopping-lists")
@RequiredArgsConstructor
public class ShoppingListController {

  private static final String AUTH_USER_HEADER = "X-Authenticated-User";

  private final ShoppingListService shoppingListService;

  /**
   * Create a new shopping list for the authenticated user.
   *
   * @param accountEmail the email of the authenticated user, injected from the gateway header
   * @param request the request body containing title and optional description
   * @return the created shopping list
   */
  @PostMapping
  public ResponseEntity<ShoppingListResponse> createShoppingList(
      @RequestHeader(AUTH_USER_HEADER) String accountEmail,
      @Valid @RequestBody CreateShoppingListRequest request) {

    ShoppingList shoppingList =
        shoppingListService.createShoppingList(
            accountEmail, request.getTitle(), request.getDescription());

    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(shoppingList));
  }

  /**
   * Get active (non-archived) shopping lists for the authenticated user.
   *
   * @param accountEmail the email of the authenticated user, injected from the gateway header
   * @return list of active shopping lists
   */
  @GetMapping("/active")
  public ResponseEntity<List<ShoppingListResponse>> getActiveShoppingLists(
      @RequestHeader(AUTH_USER_HEADER) String accountEmail) {

    List<ShoppingList> lists =
        shoppingListService.getActiveShoppingListByAccountEmail(accountEmail);

    List<ShoppingListResponse> responses =
        lists.stream().map(this::toResponse).collect(Collectors.toList());

    return ResponseEntity.ok(responses);
  }

  /**
   * Get all shopping lists (including archived) for the authenticated user.
   *
   * @param accountEmail the email of the authenticated user, injected from the gateway header
   * @return list of all shopping lists
   */
  @GetMapping("/all")
  public ResponseEntity<List<ShoppingListResponse>> getAllShoppingLists(
      @RequestHeader(AUTH_USER_HEADER) String accountEmail) {

    List<ShoppingList> lists = shoppingListService.getAllShoppingListsByAccountEmail(accountEmail);

    List<ShoppingListResponse> responses =
        lists.stream().map(this::toResponse).collect(Collectors.toList());

    return ResponseEntity.ok(responses);
  }

  /**
   * Get a specific shopping list by ID.
   *
   * @param listId the ID of the shopping list
   * @return the shopping list details
   */
  @GetMapping("/{listId}")
  public ResponseEntity<ShoppingListResponse> getShoppingListById(@PathVariable Long listId) {

    ShoppingList shoppingList = shoppingListService.getShoppingListById(listId);

    return ResponseEntity.ok(toResponse(shoppingList));
  }

  /**
   * Update a shopping list's title.
   *
   * @param listId the ID of the shopping list
   * @param request the request body containing the new title
   * @return the updated shopping list
   */
  @PutMapping("/{listId}")
  public ResponseEntity<ShoppingListResponse> updateShoppingList(
      @PathVariable Long listId, @Valid @RequestBody UpdateShoppingListRequest request) {

    ShoppingList shoppingList =
        shoppingListService.updateShoppingListTitle(listId, request.getTitle());

    return ResponseEntity.ok(toResponse(shoppingList));
  }

  /**
   * Archive a shopping list.
   *
   * @param listId the ID of the shopping list to archive
   * @return no content response
   */
  @PutMapping("/{listId}/archive")
  public ResponseEntity<Void> archiveShoppingList(@PathVariable Long listId) {

    shoppingListService.archiveShoppingList(listId);

    return ResponseEntity.noContent().build();
  }

  /**
   * Delete a shopping list.
   *
   * @param listId the ID of the shopping list to delete
   * @return no content response
   */
  @DeleteMapping("/{listId}")
  public ResponseEntity<Void> deleteShoppingList(@PathVariable Long listId) {

    shoppingListService.deleteShoppingList(listId);

    return ResponseEntity.noContent().build();
  }

  private ShoppingListResponse toResponse(ShoppingList shoppingList) {
    return ShoppingListResponse.builder()
        .listId(shoppingList.getListId())
        .title(shoppingList.getTitle())
        .description(shoppingList.getDescription())
        .isArchived(shoppingList.getIsArchived())
        .createdAt(shoppingList.getCreatedAt())
        .updatedAt(shoppingList.getUpdatedAt())
        .build();
  }
}
