package com.github.mpalambonisi.auth.repository;

import com.github.mpalambonisi.auth.model.Account;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for managing Account entities. */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
  Optional<Account> findByEmail(String email);

  boolean existsByEmail(String email);
}
