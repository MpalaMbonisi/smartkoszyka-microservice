package com.github.mpalambonisi.shoppinglist.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.mpalambonisi.common.exception.ResourceNotFoundException;
import com.github.mpalambonisi.shoppinglist.client.ProductClient;
import com.github.mpalambonisi.shoppinglist.client.ProductResponse;
import com.github.mpalambonisi.shoppinglist.model.ShoppingList;
import com.github.mpalambonisi.shoppinglist.model.ShoppingListItem;
import com.github.mpalambonisi.shoppinglist.repository.ShoppingListItemRepository;
import com.github.mpalambonisi.shoppinglist.repository.ShoppingListRepository;
import com.github.mpalambonisi.shoppinglist.service.impl.ShoppingListItemServiceImpl;
import java.math.BigDecimal;
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
@DisplayName("ShoppingListItemService Tests")
public class ShoppingListItemServiceImplTest {

  @Mock private ShoppingListItemRepository shoppingListItemRepository;

  @Mock private ShoppingListRepository shoppingListRepository;

  @Mock private ProductClient productClient;

  @InjectMocks private ShoppingListItemServiceImpl shoppingListItemService;

  private ShoppingList testShoppingList;
  private ShoppingListItem testItem;
  private ProductResponse testProductResponse;
  private Long listId;
  private Long productId;
  private Long itemId;

  @BeforeEach
  void setUp() {
    listId = 100L;
    productId = 200L;
    itemId = 300L;

    testShoppingList = new ShoppingList();
    testShoppingList.setListId(listId);
    testShoppingList.setTitle("Weekly Shopping");
    testShoppingList.setIsArchived(false);

    testProductResponse = new ProductResponse();
    testProductResponse.setId(productId);
    testProductResponse.setName("Pomidory");
    testProductResponse.setPrice(new BigDecimal("5.99"));
    testProductResponse.setUnit("kg");

    testItem = new ShoppingListItem();
    testItem.setListItemId(itemId);
    testItem.setShoppingList(testShoppingList);
    testItem.setProductId(testProductResponse.getId());
    testItem.setProductName(testProductResponse.getName());
    testItem.setQuantity(2);
    testItem.setUnit("kg");
    testItem.setPriceAtAddition(new BigDecimal("5.99"));
    testItem.setIsChecked(false);
  }

  @Test
  @DisplayName("Should add product to list successfully")
  void shouldAddProductToListSuccessfully() {
    // Given
    Integer quantity = 2;
    when(shoppingListRepository.findById(listId)).thenReturn(Optional.of(testShoppingList));
    when(productClient.getProductById(productId)).thenReturn(testProductResponse);
    when(shoppingListItemRepository.existsByShoppingListListIdAndProductId(listId, productId))
        .thenReturn(false);
    when(shoppingListItemRepository.save(any(ShoppingListItem.class))).thenReturn(testItem);

    // When
    ShoppingListItem result = shoppingListItemService.addProductToList(listId, productId, quantity);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getQuantity()).isEqualTo(quantity);
    assertThat(result.getProductId()).isEqualTo(testProductResponse.getId());
    assertThat(result.getProductName()).isEqualTo(testProductResponse.getName());
    assertThat(result.getShoppingList()).isEqualTo(testShoppingList);
    assertThat(result.getUnit()).isEqualTo(testProductResponse.getUnit());
    assertThat(result.getPriceAtAddition()).isEqualTo(testProductResponse.getPrice());
    assertThat(result.getIsChecked()).isFalse();

    // Verify
    verify(shoppingListRepository).findById(listId);
    verify(productClient).getProductById(productId);
    verify(shoppingListItemRepository).save(any(ShoppingListItem.class));
  }

  @Test
  @DisplayName("Should throw exception when shopping list not found")
  void shouldThrowExceptionWhenShoppingListNotFound() {
    // Given
    when(shoppingListRepository.findById(listId)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> shoppingListItemService.addProductToList(listId, productId, 2))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Shopping list not found with id: 100");

    // Verify
    verify(shoppingListRepository).findById(listId);
    verify(productClient, never()).getProductById(anyLong());
    verify(shoppingListItemRepository, never()).save(any(ShoppingListItem.class));
  }

  @Test
  @DisplayName("Should throw exception when product not found")
  void shouldThrowExceptionWhenProductNotFound() {
    // Given
    Long nonExistentId = 999L;

    // Mock
    when(shoppingListRepository.findById(listId)).thenReturn(Optional.of(testShoppingList));
    when(shoppingListItemRepository.existsByShoppingListListIdAndProductId(listId, nonExistentId))
        .thenReturn(false);

    when(productClient.getProductById(nonExistentId))
        .thenThrow(new ResourceNotFoundException("Product not found with id: " + nonExistentId));

    // When & Then
    assertThatThrownBy(() -> shoppingListItemService.addProductToList(listId, nonExistentId, 2))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Product not found with id: 999");

    // Verify
    verify(shoppingListRepository).findById(listId);
    verify(productClient).getProductById(nonExistentId);
    verify(shoppingListItemRepository, never()).save(any(ShoppingListItem.class));
  }

  @Test
  @DisplayName("Should throw exception when product already in list")
  void shouldThrowExceptionWhenProductAlreadyInList() {
    // Given
    when(shoppingListRepository.findById(listId)).thenReturn(Optional.of(testShoppingList));
    when(shoppingListItemRepository.existsByShoppingListListIdAndProductId(listId, productId))
        .thenReturn(true);

    // When & Then
    assertThatThrownBy(() -> shoppingListItemService.addProductToList(listId, productId, 2))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Product is already in this shopping list");

    // Verify
    verify(shoppingListRepository).findById(listId);
    verify(productClient, never()).getProductById(anyLong());
    verify(shoppingListItemRepository, never()).save(any(ShoppingListItem.class));
  }

  @Test
  @DisplayName("Should throw exception when quantity is null")
  void shouldThrowExceptionWhenQuantityIsNull() {
    // When & Then
    assertThatThrownBy(() -> shoppingListItemService.addProductToList(listId, productId, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Quantity must be greater than 0");

    verify(shoppingListRepository, never()).findById(anyLong());
  }

  @Test
  @DisplayName("Should throw exception when quantity is zero")
  void shouldThrowExceptionWhenQuantityIsZero() {
    // When & Then
    assertThatThrownBy(() -> shoppingListItemService.addProductToList(listId, productId, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Quantity must be greater than 0");
  }

  @Test
  @DisplayName("Should throw exception when quantity is negative")
  void shouldThrowExceptionWhenQuantityIsNegative() {
    // When & Then
    assertThatThrownBy(() -> shoppingListItemService.addProductToList(listId, productId, -5))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Quantity must be greater than 0");
  }

  @Test
  @DisplayName("Should update quantity successfully")
  void shouldUpdateQuantitySuccessfully() {
    // Given
    Integer newQuantity = 5;
    when(shoppingListItemRepository.findById(itemId)).thenReturn(Optional.of(testItem));
    when(shoppingListItemRepository.save(any(ShoppingListItem.class))).thenReturn(testItem);

    // When
    ShoppingListItem result = shoppingListItemService.updateQuantity(itemId, newQuantity);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getQuantity()).isEqualTo(newQuantity);
    verify(shoppingListItemRepository).findById(itemId);
    verify(shoppingListItemRepository).save(testItem);
  }

  @Test
  @DisplayName("Should throw exception when updating quantity of non-existent item")
  void shouldThrowExceptionWhenUpdatingQuantityOfNonExistentItem() {
    // Given
    when(shoppingListItemRepository.findById(itemId)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> shoppingListItemService.updateQuantity(itemId, 5))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Shopping list item not found with id: 300");

    verify(shoppingListItemRepository).findById(itemId);
    verify(shoppingListItemRepository, never()).save(any(ShoppingListItem.class));
  }

  @Test
  @DisplayName("Should throw exception when updating with invalid quantity")
  void shouldThrowExceptionWhenUpdatingWithInvalidQuantity() {
    // When & Then
    assertThatThrownBy(() -> shoppingListItemService.updateQuantity(itemId, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Quantity must be greater than 0");

    verify(shoppingListItemRepository, never()).findById(anyLong());
  }

  @Test
  @DisplayName("Should toggle item to checked")
  void shouldToggleItemToChecked() {
    // Given
    testItem.setIsChecked(false);
    when(shoppingListItemRepository.findById(itemId)).thenReturn(Optional.of(testItem));
    when(shoppingListItemRepository.save(any(ShoppingListItem.class))).thenReturn(testItem);

    // When
    shoppingListItemService.toggleItemChecked(itemId);

    // Then
    assertThat(testItem.getIsChecked()).isTrue();
    verify(shoppingListItemRepository).findById(itemId);
    verify(shoppingListItemRepository).save(testItem);
  }

  @Test
  @DisplayName("Should toggle item to unchecked")
  void shouldToggleItemToUnchecked() {
    // Given
    testItem.setIsChecked(true);
    when(shoppingListItemRepository.findById(itemId)).thenReturn(Optional.of(testItem));
    when(shoppingListItemRepository.save(any(ShoppingListItem.class))).thenReturn(testItem);

    // When
    shoppingListItemService.toggleItemChecked(itemId);

    // Then
    assertThat(testItem.getIsChecked()).isFalse();
    verify(shoppingListItemRepository).findById(itemId);
    verify(shoppingListItemRepository).save(testItem);
  }

  @Test
  @DisplayName("Should throw exception when toggling non-existent item")
  void shouldThrowExceptionWhenTogglingNonExistentItem() {
    // Given
    when(shoppingListItemRepository.findById(itemId)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> shoppingListItemService.toggleItemChecked(itemId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Shopping list item not found with id: 300");

    // Verify
    verify(shoppingListItemRepository).findById(itemId);
    verify(shoppingListItemRepository, never()).save(any(ShoppingListItem.class));
  }

  @Test
  @DisplayName("Should remove item from list successfully")
  void shouldRemoveItemFromListSuccessfully() {
    // Given
    when(shoppingListItemRepository.findById(itemId)).thenReturn(Optional.of(testItem));

    // When
    shoppingListItemService.removeItemFromList(itemId);

    // Then
    verify(shoppingListItemRepository).findById(itemId);
    verify(shoppingListItemRepository).delete(testItem);
  }

  @Test
  @DisplayName("Should throw exception when removing non-existent item")
  void shouldThrowExceptionWhenRemovingNonExistentItem() {
    // Given
    when(shoppingListItemRepository.findById(itemId)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> shoppingListItemService.removeItemFromList(itemId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Shopping list item not found with id: 300");

    // Verify
    verify(shoppingListItemRepository).findById(itemId);
    verify(shoppingListItemRepository, never()).delete(any(ShoppingListItem.class));
  }

  @Test
  @DisplayName("Should get items by list ID successfully")
  void shouldGetItemsByListIdSuccessfully() {
    // Given
    ShoppingListItem item2 = new ShoppingListItem();
    item2.setListItemId(301L);
    item2.setProductId(testProductResponse.getId());
    item2.setProductName(testProductResponse.getName());
    item2.setQuantity(3);

    List<ShoppingListItem> items = Arrays.asList(testItem, item2);
    when(shoppingListItemRepository.findByShoppingListListId(listId)).thenReturn(items);

    // When
    List<ShoppingListItem> result = shoppingListItemService.getItemsByListId(listId);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result).contains(testItem, item2);

    // Verify
    verify(shoppingListItemRepository).findByShoppingListListId(listId);
  }

  @Test
  @DisplayName("Should return empty list when no items in shopping list")
  void shouldReturnEmptyListWhenNoItemsInShoppingList() {
    // Given
    when(shoppingListItemRepository.findByShoppingListListId(listId)).thenReturn(List.of());

    // When
    List<ShoppingListItem> result = shoppingListItemService.getItemsByListId(listId);

    // Then
    assertThat(result).isEmpty();

    // Verify
    verify(shoppingListItemRepository).findByShoppingListListId(listId);
  }
}
