package com.github.mpalambonisi.auth.service;

import com.github.mpalambonisi.auth.model.Account;
import com.github.mpalambonisi.auth.repository.AccountRepository;
import com.github.mpalambonisi.common.exception.EmailAlreadyExistsException;
import com.github.mpalambonisi.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementation of the {@link AccountService} interface. */
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

  private final AccountRepository accountRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public Account registerAccount(String email, String password, String firstName, String lastName) {

    // Validate inputs
    validateRegistrationInputs(email, password, firstName, lastName);

    // Normalise email (lowercase and trim)
    String normalisedEmail = email.toLowerCase().trim();

    // Check if email already exists
    if (accountRepository.existsByEmail(normalisedEmail)) {
      throw new EmailAlreadyExistsException("Email already exists: " + normalisedEmail);
    }

    // Create new account
    Account account = new Account();
    account.setEmail(normalisedEmail);
    account.setPasswordHash(passwordEncoder.encode(password));
    account.setFirstName(firstName);
    account.setLastName(lastName);

    // Save and return
    return accountRepository.save(account);
  }

  @Override
  @Transactional(readOnly = true)
  public Account getAccountById(Long accountId) {
    return accountRepository
        .findById(accountId)
        .orElseThrow(
            () -> new ResourceNotFoundException("Account not found with id: " + accountId));
  }

  @Override
  @Transactional(readOnly = true)
  public Account getAccountByEmail(String email) {
    String normalisedEmail = email.toLowerCase().trim();
    return accountRepository
        .findByEmail(normalisedEmail)
        .orElseThrow(
            () ->
                new ResourceNotFoundException("Account not found with email: " + normalisedEmail));
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsByEmail(String email) {
    String normalisedEmail = email.toLowerCase().trim();
    return accountRepository.existsByEmail(normalisedEmail);
  }

  /**
   * Validates registration inputs to ensure they are not null or empty.
   *
   * @param email The email to validate
   * @param password The password to validate
   * @param firstName The first name to validate
   * @param lastName The last name to validate
   * @throws IllegalArgumentException if any input is null or empty
   */
  private void validateRegistrationInputs(
      String email, String password, String firstName, String lastName) {

    if (email == null || email.trim().isEmpty()) {
      throw new IllegalArgumentException("Email cannot be null or empty");
    }
    if (password == null || password.trim().isEmpty()) {
      throw new IllegalArgumentException("Password cannot be null or empty");
    }
    if (firstName == null || firstName.trim().isEmpty()) {
      throw new IllegalArgumentException("First name cannot be null or empty");
    }
    if (lastName == null || lastName.trim().isEmpty()) {
      throw new IllegalArgumentException("Last name cannot be null or empty");
    }
  }

  /**
   * Deletes account by email.
   *
   * @param email The email to delete
   * @throws ResourceNotFoundException if any account with email is not found
   */
  @Override
  @Transactional
  public void deleteAccount(String email) {
    // Normalise email (lowercase and trim)
    String normalisedEmail = email.toLowerCase().trim();

    Account accountToDelete =
        accountRepository
            .findByEmail(normalisedEmail)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Account not found with email: " + normalisedEmail));

    accountRepository.deleteById(accountToDelete.getAccountId());
  }
}
