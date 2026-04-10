package com.github.mpalambonisi.auth.service;

import com.github.mpalambonisi.auth.model.Account;

/** Service interface for managing user accounts. */
public interface AccountService {

  Account registerAccount(String email, String password, String firstName, String lastName);

  Account getAccountById(Long accountId);

  Account getAccountByEmail(String email);

  boolean existsByEmail(String email);

  void deleteAccount(String email);
}
