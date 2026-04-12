package com.github.mpalambonisi.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mpalambonisi.auth.repository.AccountRepository;
import com.github.mpalambonisi.common.dto.request.AccountRegistrationRequest;
import com.github.mpalambonisi.common.dto.request.DeleteAccountRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Account Controller Integration Tests")
public class AccountControllerIT {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:15-alpine")
          .withDatabaseName("testdb")
          .withUsername("test")
          .withPassword("test");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AccountRepository accountRepository;

  @BeforeEach
  void setUp() {
    accountRepository.deleteAll();
  }

  @Test
  @WithMockUser(username = "nicole.smith@example.com")
  @DisplayName("Should delete own account successfully")
  void shouldDeleteOwnAccountSuccessfully() throws Exception {
    // Given - Pre-register the account in the DB
    String email = "nicole.smith@example.com";
    registerTestAccount(email);

    DeleteAccountRequest deleteRequest = new DeleteAccountRequest(email);

    // When & Then
    mockMvc
        .perform(
            delete("/api/account/delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deleteRequest)))
        .andExpect(status().isNoContent());

    // Verify account is removed from DB
    assertThat(accountRepository.findByEmail(email)).isEmpty();
  }

  @Test
  @WithMockUser(username = "attacker@example.com")
  @DisplayName("Should return forbidden when trying to delete another user's account")
  void shouldReturnForbiddenWhenDeletingOtherAccount() throws Exception {
    // Given - A victim exists in the DB
    String victimEmail = "victim@example.com";
    registerTestAccount(victimEmail);

    DeleteAccountRequest maliciousRequest = new DeleteAccountRequest(victimEmail);

    // When & Then
    mockMvc
        .perform(
            delete("/api/account/delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(maliciousRequest)))
        .andExpect(status().isForbidden());

    // Verify victim still exists
    assertThat(accountRepository.findByEmail(victimEmail)).isPresent();
  }

  @Test
  @WithMockUser(username = "nicole.smith@example.com")
  @DisplayName("Should return not found when account to delete does not exist")
  void shouldReturnNotFoundWhenAccountDoesNotExist() throws Exception {
    // Given - Authenticated as Nicole, but requesting to delete an email not in DB
    String nonExistentEmail =
        "nicole.smith@example.com"; // Authenticated name matches but DB is empty
    DeleteAccountRequest deleteRequest = new DeleteAccountRequest(nonExistentEmail);

    // When & Then
    mockMvc
        .perform(
            delete("/api/account/delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deleteRequest)))
        .andExpect(status().isNotFound());
  }

  /** Helper method to populate the database via the existing auth flow */
  private void registerTestAccount(String email) throws Exception {
    AccountRegistrationRequest request =
        AccountRegistrationRequest.builder()
            .firstName("Nicole")
            .lastName("Smith")
            .email(email)
            .password("StrongPassword1234")
            .build();

    mockMvc.perform(
        post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));
  }
}
