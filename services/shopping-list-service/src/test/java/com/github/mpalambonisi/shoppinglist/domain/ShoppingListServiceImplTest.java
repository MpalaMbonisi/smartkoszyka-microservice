package com.github.mpalambonisi.shoppinglist.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.mpalambonisi.common.exception.ResourceNotFoundException;
import com.github.mpalambonisi.shoppinglist.model.ShoppingList;
import com.github.mpalambonisi.shoppinglist.repository.ShoppingListRepository;
import com.github.mpalambonisi.shoppinglist.service.impl.ShoppingListServiceImpl;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShoppingListService Tests")
public class ShoppingListServiceImplTest {

  @Mock private ShoppingListRepository shoppingListRepository;

  @InjectMocks private ShoppingListServiceImpl shoppingListService;

  private ShoppingList testShoppingList;
  private String accountEmail;
  private Long listId;

  @BeforeEach
  void setUp() {
    accountEmail = "nicole.smith@example.com";
    listId = 100L;

    testShoppingList = new ShoppingList();
    testShoppingList.setListId(listId);
    testShoppingList.setAccountEmail(accountEmail);
    testShoppingList.setTitle("Weekly Shopping");
    testShoppingList.setDescription("Groceries for the week");
    testShoppingList.setIsArchived(false);
  }

  @Test
  @DisplayName("Should create shopping list successfully")
  void shouldCreateShoppingListSuccessfully() {
    // Given
    String title = "Weekly Shopping";
    String description = "Groceries for the week";
    when(shoppingListRepository.save(any(ShoppingList.class))).thenReturn(testShoppingList);

    // When
    ShoppingList result = shoppingListService.createShoppingList(accountEmail, title, description);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getTitle()).isEqualTo(title);
    assertThat(result.getDescription()).isEqualTo(description);
    assertThat(result.getAccountEmail()).isEqualTo(accountEmail);
    assertThat(result.getIsArchived()).isFalse();

    // Verify
    verify(shoppingListRepository).save(any(ShoppingList.class));
  }

  @Test
  @DisplayName("Should create shopping list with null description")
  void shouldCreateShoppingListWithNullDescription() {
    // Given
    String title = "Quick List";
    when(shoppingListRepository.save(any(ShoppingList.class))).thenReturn(testShoppingList);

    // When
    ShoppingList result = shoppingListService.createShoppingList(accountEmail, title, null);

    // Then
    assertThat(result).isNotNull();
    verify(shoppingListRepository).save(any(ShoppingList.class));
  }

  @Test
  @DisplayName("Should throw exception when title is null")
  void shouldThrowExceptionWhenTitleIsNull() {
    // When & Then
    assertThatThrownBy(() -> shoppingListService.createShoppingList(accountEmail, null, "Desc"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Title cannot be null or empty");
  }

  @Test
  @DisplayName("Should throw exception when title is empty")
  void shouldThrowExceptionWhenTitleIsEmpty() {
    // When & Then
    assertThatThrownBy(() -> shoppingListService.createShoppingList(accountEmail, "  ", "Desc"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Title cannot be null or empty");
  }

  @Test
  @DisplayName("Should get shopping list by ID successfully")
  void shouldGetShoppingListByIdSuccessfully() {
    // Given
    when(shoppingListRepository.findById(listId)).thenReturn(Optional.of(testShoppingList));

    // When
    ShoppingList result = shoppingListService.getShoppingListById(listId);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getListId()).isEqualTo(listId);
    assertThat(result.getTitle()).isEqualTo("Weekly Shopping");
    verify(shoppingListRepository).findById(listId);
  }

  @Test
  @DisplayName("Should throw exception when shopping list not found by ID")
  void shouldThrowExceptionWhenShoppingListNotFoundById() {
    // Given
    when(shoppingListRepository.findById(listId)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> shoppingListService.getShoppingListById(listId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Shopping list not found with id: 100");

    verify(shoppingListRepository).findById(listId);
  }

  @Test
  @DisplayName("Should get active shopping lists by account email")
  void shouldGetActiveShoppingListsByAccount() {
    // Given
    ShoppingList list2 = new ShoppingList();
    list2.setListId(101L);
    list2.setTitle("Monthly List");
    list2.setIsArchived(false);

    List<ShoppingList> activeLists = Arrays.asList(testShoppingList, list2);
    when(shoppingListRepository.findByAccountEmailAndIsArchivedFalse(accountEmail))
        .thenReturn(activeLists);

    // When
    List<ShoppingList> result =
        shoppingListService.getActiveShoppingListByAccountEmail(accountEmail);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result).allMatch(list -> !list.getIsArchived());
    verify(shoppingListRepository).findByAccountEmailAndIsArchivedFalse(accountEmail);
  }

  @Test
  @DisplayName("Should return empty list when no active shopping lists")
  void shouldReturnEmptyListWhenNoActiveShoppingLists() {
    // Given
    when(shoppingListRepository.findByAccountEmailAndIsArchivedFalse(accountEmail))
        .thenReturn(List.of());

    // When
    List<ShoppingList> result =
        shoppingListService.getActiveShoppingListByAccountEmail(accountEmail);

    // Then
    assertThat(result).isEmpty();
    verify(shoppingListRepository).findByAccountEmailAndIsArchivedFalse(accountEmail);
  }

  @Test
  @DisplayName("Should get all shopping lists by account email including archived")
  void shouldGetAllShoppingListsByAccount() {
    // Given
    ShoppingList archivedList = new ShoppingList();
    archivedList.setListId(102L);
    archivedList.setTitle("Old List");
    archivedList.setIsArchived(true);

    List<ShoppingList> allLists = Arrays.asList(testShoppingList, archivedList);
    when(shoppingListRepository.findByAccountEmail(accountEmail)).thenReturn(allLists);

    // When
    List<ShoppingList> result = shoppingListService.getAllShoppingListsByAccountEmail(accountEmail);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result).anyMatch(ShoppingList::getIsArchived);
    assertThat(result).anyMatch(list -> !list.getIsArchived());
    verify(shoppingListRepository).findByAccountEmail(accountEmail);
  }

  @Test
  @DisplayName("Should update shopping list title successfully")
  void shouldUpdateShoppingListTitleSuccessfully() {
    // Given
    String newTitle = "Updated Title";
    when(shoppingListRepository.findById(listId)).thenReturn(Optional.of(testShoppingList));
    when(shoppingListRepository.save(any(ShoppingList.class))).thenReturn(testShoppingList);

    // When
    ShoppingList result = shoppingListService.updateShoppingListTitle(listId, newTitle);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getTitle()).isEqualTo(newTitle);
    verify(shoppingListRepository).findById(listId);
    verify(shoppingListRepository).save(testShoppingList);
  }

  @Test
  @DisplayName("Should throw exception when updating with null title")
  void shouldThrowExceptionWhenUpdatingWithNullTitle() {
    // When & Then
    assertThatThrownBy(() -> shoppingListService.updateShoppingListTitle(listId, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Title cannot be null or empty");

    verify(shoppingListRepository, never()).findById(anyLong());
  }

  @Test
  @DisplayName("Should throw exception when updating with empty title")
  void shouldThrowExceptionWhenUpdatingWithEmptyTitle() {
    // When & Then
    assertThatThrownBy(() -> shoppingListService.updateShoppingListTitle(listId, "   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Title cannot be null or empty");
  }

  @Test
  @DisplayName("Should throw exception when list not found during update")
  void shouldThrowExceptionWhenListNotFoundDuringUpdate() {
    // Given
    when(shoppingListRepository.findById(listId)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> shoppingListService.updateShoppingListTitle(listId, "New Title"))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Shopping list not found with id: 100");

    verify(shoppingListRepository).findById(listId);
    verify(shoppingListRepository, never()).save(any(ShoppingList.class));
  }

  @Test
  @DisplayName("Should archive shopping list successfully")
  void shouldArchiveShoppingListSuccessfully() {
    // Given
    when(shoppingListRepository.findById(listId)).thenReturn(Optional.of(testShoppingList));
    when(shoppingListRepository.save(any(ShoppingList.class))).thenReturn(testShoppingList);

    // When
    shoppingListService.archiveShoppingList(listId);

    // Then
    assertThat(testShoppingList.getIsArchived()).isTrue();
    verify(shoppingListRepository).findById(listId);
    verify(shoppingListRepository).save(testShoppingList);
  }

  @Test
  @DisplayName("Should throw exception when archiving non-existent list")
  void shouldThrowExceptionWhenArchivingNonExistentList() {
    // Given
    when(shoppingListRepository.findById(listId)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> shoppingListService.archiveShoppingList(listId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Shopping list not found with id: 100");

    verify(shoppingListRepository).findById(listId);
    verify(shoppingListRepository, never()).save(any(ShoppingList.class));
  }

  @Test
  @DisplayName("Should delete shopping list successfully")
  void shouldDeleteShoppingListSuccessfully() {
    // Given
    when(shoppingListRepository.findById(listId)).thenReturn(Optional.of(testShoppingList));

    // When
    shoppingListService.deleteShoppingList(listId);

    // Then
    verify(shoppingListRepository).findById(listId);
    verify(shoppingListRepository).delete(testShoppingList);
  }

  @Test
  @DisplayName("Should throw exception when deleting non-existent list")
  void shouldThrowExceptionWhenDeletingNonExistentList() {
    // Given
    when(shoppingListRepository.findById(listId)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> shoppingListService.deleteShoppingList(listId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Shopping list not found with id: 100");

    verify(shoppingListRepository).findById(listId);
    verify(shoppingListRepository, never()).delete(any(ShoppingList.class));
  }
}
