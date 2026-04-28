package com.github.mpalambonisi.shoppinglist.repository;

import com.github.mpalambonisi.shoppinglist.model.ShoppingList;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for managing shopping list entities. */
@Repository
public interface ShoppingListRepository extends JpaRepository<ShoppingList, Long> {
  List<ShoppingList> findByAccountEmailAndIsArchivedFalse(String accountEmail);

  List<ShoppingList> findByAccountEmail(String accountEmail);
}
