package com.github.mpalambonisi.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.mpalambonisi.auth.model.Account;
import com.github.mpalambonisi.auth.repository.AccountRepository;
import com.github.mpalambonisi.auth.service.AccountServiceImpl;
import com.github.mpalambonisi.common.exception.EmailAlreadyExistsException;
import com.github.mpalambonisi.common.exception.ResourceNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService Tests")
public class AccountServiceImplTest {

  @Mock private AccountRepository accountRepository;

  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks private AccountServiceImpl accountService;

  private Account testAccount;
  private String testEmail;
  private String testPassword;
  private String testFirstName;
  private String testLastName;

  @BeforeEach
  void setUp() {
    testEmail = "nicolesmith@example.com";
    testPassword = "StrongPassword1234";
    testFirstName = "Nicole";
    testLastName = "Smith";

    testAccount = new Account();
    testAccount.setAccountId(1L);
    testAccount.setEmail(testEmail);
    testAccount.setPasswordHash(testPassword);
    testAccount.setFirstName(testFirstName);
    testAccount.setLastName(testLastName);
  }

  @Test
  @DisplayName("Should register new account successfully")
  void shouldRegisterAccountSuccessfully() {
    // Given
    when(accountRepository.existsByEmail(testEmail.toLowerCase().trim())).thenReturn(false);
    when(passwordEncoder.encode(testPassword)).thenReturn("hashedStrongPassword1234");
    when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

    // When
    Account result =
        accountService.registerAccount(testEmail, testPassword, testFirstName, testLastName);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getEmail()).isEqualTo(testEmail);
    assertThat(result.getFirstName()).isEqualTo(testFirstName);
    assertThat(result.getLastName()).isEqualTo(testLastName);

    // Verify
    verify(accountRepository).existsByEmail(testEmail.toLowerCase().trim());
    verify(passwordEncoder).encode(testPassword);
    verify(accountRepository).save(any(Account.class));
  }

  @Test
  @DisplayName("Should normalize email to lowercase when registering")
  void shouldNormalizeEmailToLowercase() {
    // Given
    String mixedCaseEmail = "NicoleSmith@Example.COM";
    when(accountRepository.existsByEmail(mixedCaseEmail.toLowerCase().trim())).thenReturn(false);
    when(passwordEncoder.encode(testPassword)).thenReturn("hashedPassword");
    when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

    // When
    accountService.registerAccount(mixedCaseEmail, testPassword, testFirstName, testLastName);

    // Then
    verify(accountRepository).existsByEmail("nicolesmith@example.com");
  }

  @Test
  @DisplayName("Should trim whitespace from email when registering")
  void shouldTrimEmailWhitespace() {
    // Given
    String emailWithSpaces = "  nicolesmith@example.com  ";
    when(accountRepository.existsByEmail(emailWithSpaces.toLowerCase().trim())).thenReturn(false);
    when(passwordEncoder.encode(testPassword)).thenReturn("hashedPassword");
    when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

    // When
    accountService.registerAccount(emailWithSpaces, testPassword, testFirstName, testLastName);

    // Then
    verify(accountRepository).existsByEmail("nicolesmith@example.com");
  }

  @Test
  @DisplayName("Should throw EmailAlreadyExistsException when email exists")
  void shouldThrowExceptionWhenEmailExists() {
    // Given
    when(accountRepository.existsByEmail(testEmail.toLowerCase().trim())).thenReturn(true);

    // When & Then
    assertThatThrownBy(
            () ->
                accountService.registerAccount(
                    testEmail, testPassword, testFirstName, testLastName))
        .isInstanceOf(EmailAlreadyExistsException.class)
        .hasMessageContaining("Email already exists");

    verify(accountRepository).existsByEmail(testEmail.toLowerCase().trim());
    verify(passwordEncoder, never()).encode(anyString());
    verify(accountRepository, never()).save(any(Account.class));
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException when email is null")
  void shouldThrowExceptionWhenEmailIsNull() {
    // When & Then
    assertThatThrownBy(
            () -> accountService.registerAccount(null, testPassword, testFirstName, testLastName))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Email cannot be null or empty");

    verify(accountRepository, never()).existsByEmail(anyString());
    verify(accountRepository, never()).save(any(Account.class));
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException when email is empty")
  void shouldThrowExceptionWhenEmailIsEmpty() {
    // When & Then
    assertThatThrownBy(
            () -> accountService.registerAccount("", testPassword, testFirstName, testLastName))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Email cannot be null or empty");
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException when password is null")
  void shouldThrowExceptionWhenPasswordIsNull() {
    // When & Then
    assertThatThrownBy(
            () -> accountService.registerAccount(testEmail, null, testFirstName, testLastName))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Password cannot be null or empty");
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException when firstName is null")
  void shouldThrowExceptionWhenFirstNameIsNull() {
    // When & Then
    assertThatThrownBy(
            () -> accountService.registerAccount(testEmail, testPassword, null, testLastName))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("First name cannot be null or empty");
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException when lastName is null")
  void shouldThrowExceptionWhenLastNameIsNull() {
    // When & Then
    assertThatThrownBy(
            () -> accountService.registerAccount(testEmail, testPassword, testFirstName, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Last name cannot be null or empty");
  }

  @Test
  @DisplayName("Should get account by ID successfully")
  void shouldGetAccountByIdSuccessfully() {
    // Given
    Long accountId = 1L;
    when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));

    // When
    Account result = accountService.getAccountById(accountId);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getAccountId()).isEqualTo(accountId);
    assertThat(result.getEmail()).isEqualTo(testEmail);
    verify(accountRepository).findById(accountId);
  }

  @Test
  @DisplayName("Should throw ResourceNotFoundException when account ID not found")
  void shouldThrowExceptionWhenAccountIdNotFound() {
    // Given
    Long accountId = 999L;
    when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> accountService.getAccountById(accountId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Account not found with id: 999");

    verify(accountRepository).findById(accountId);
  }

  @Test
  @DisplayName("Should get account by email successfully")
  void shouldGetAccountByEmailSuccessfully() {
    // Given
    when(accountRepository.findByEmail(testEmail.toLowerCase().trim()))
        .thenReturn(Optional.of(testAccount));

    // When
    Account result = accountService.getAccountByEmail(testEmail);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getEmail()).isEqualTo(testEmail);
    verify(accountRepository).findByEmail(testEmail.toLowerCase().trim());
  }

  @Test
  @DisplayName("Should normalize email when getting account by email")
  void shouldNormalizeEmailWhenGettingByEmail() {
    // Given
    String mixedCaseEmail = "NicoleSmith@Example.COM";
    when(accountRepository.findByEmail(mixedCaseEmail.toLowerCase().trim()))
        .thenReturn(Optional.of(testAccount));

    // When
    accountService.getAccountByEmail(mixedCaseEmail);

    // Then
    verify(accountRepository).findByEmail("nicolesmith@example.com");
  }

  @Test
  @DisplayName("Should throw ResourceNotFoundException when email not found")
  void shouldThrowExceptionWhenEmailNotFound() {
    // Given
    String nonExistentEmail = "notfound@example.com";
    when(accountRepository.findByEmail(nonExistentEmail.toLowerCase().trim()))
        .thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> accountService.getAccountByEmail(nonExistentEmail))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Account not found with email: notfound@example.com");

    verify(accountRepository).findByEmail(nonExistentEmail.toLowerCase().trim());
  }

  @Test
  @DisplayName("Should return true when email exists")
  void shouldReturnTrueWhenEmailExists() {
    // Given
    when(accountRepository.existsByEmail(testEmail.toLowerCase().trim())).thenReturn(true);

    // When
    boolean result = accountService.existsByEmail(testEmail);

    // Then
    assertThat(result).isTrue();
    verify(accountRepository).existsByEmail(testEmail.toLowerCase().trim());
  }

  @Test
  @DisplayName("Should return false when email does not exist")
  void shouldReturnFalseWhenEmailDoesNotExist() {
    // Given
    when(accountRepository.existsByEmail(testEmail.toLowerCase().trim())).thenReturn(false);

    // When
    boolean result = accountService.existsByEmail(testEmail);

    // Then
    assertThat(result).isFalse();
    verify(accountRepository).existsByEmail(testEmail.toLowerCase().trim());
  }

  @Test
  @DisplayName("Should delete account by email successfully")
  void shouldDeleteAccountSuccessfully() {
    // Given
    String normalisedEmail = testEmail.toLowerCase().trim();
    when(accountRepository.findByEmail(normalisedEmail)).thenReturn(Optional.of(testAccount));

    // When
    accountService.deleteAccount(testEmail);

    // Then
    verify(accountRepository).findByEmail(normalisedEmail);
    verify(accountRepository).deleteById(testAccount.getAccountId());
  }

  @Test
  @DisplayName("Should normalize email when deleting account")
  void shouldNormalizeEmailWhenDeleting() {
    // Given
    String mixedCaseEmail = "  NicoleSmith@Example.COM  ";
    String normalisedEmail = "nicolesmith@example.com";
    when(accountRepository.findByEmail(normalisedEmail)).thenReturn(Optional.of(testAccount));

    // When
    accountService.deleteAccount(mixedCaseEmail);

    // Then
    verify(accountRepository).findByEmail(normalisedEmail);
  }

  @Test
  @DisplayName("Should throw ResourceNotFoundException when deleting non-existent email")
  void shouldThrowExceptionWhenDeletingNonExistentEmail() {
    // Given
    String nonExistentEmail = "notfound@example.com";
    String normalisedEmail = nonExistentEmail.toLowerCase().trim();
    when(accountRepository.findByEmail(normalisedEmail)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> accountService.deleteAccount(nonExistentEmail))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Account not found with email: " + normalisedEmail);

    verify(accountRepository).findByEmail(normalisedEmail);
    verify(accountRepository, never()).deleteById(anyLong());
  }
}
