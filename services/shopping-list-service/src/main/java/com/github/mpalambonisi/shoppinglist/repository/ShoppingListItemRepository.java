package com.github.mpalambonisi.shoppinglist.repository;

import com.github.mpalambonisi.shoppinglist.model.ShoppingListItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for managing shopping list item entities. */
@Repository
public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItem, Long> {
  List<ShoppingListItem> findByShoppingListListId(Long listId);

  boolean existsByShoppingListListIdAndProductId(Long listId, Long productId);
}
