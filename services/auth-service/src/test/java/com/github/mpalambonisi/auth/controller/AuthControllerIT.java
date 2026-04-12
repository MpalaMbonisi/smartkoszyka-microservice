package com.github.mpalambonisi.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mpalambonisi.auth.repository.AccountRepository;
import com.github.mpalambonisi.common.dto.request.AccountRegistrationRequest;
import com.github.mpalambonisi.common.dto.request.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Authentication Controller Integration Tests")
public class AuthControllerIT {

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
  @DisplayName("Should register new account successfully")
  void shouldRegisterNewAccountSuccessfully() throws Exception {
    // Given
    AccountRegistrationRequest request =
        AccountRegistrationRequest.builder()
            .firstName("Nicole")
            .lastName("Smith")
            .email("nicole.smith@example.com")
            .password("StrongPassword1234")
            .build();

    // When & Then
    mockMvc
        .perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.token").exists())
        .andExpect(jsonPath("$.email").value("nicole.smith@example.com"))
        .andExpect(jsonPath("$.firstName").value("Nicole"))
        .andExpect(jsonPath("$.lastName").value("Smith"));

    // Verify account was saved
    assertThat(accountRepository.findByEmail("nicole.smith@example.com")).isPresent();
  }

  @Test
  @DisplayName("Should normalize email to lowercase during registration")
  void shouldNormalizeEmailToLowercaseDuringRegistration() throws Exception {
    // Given
    AccountRegistrationRequest request =
        AccountRegistrationRequest.builder()
            .firstName("Jane")
            .lastName("Smith")
            .email("Jane.Smith@EXAMPLE.COM")
            .password("StrongPassword1234")
            .build();

    // When & Then
    mockMvc
        .perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("jane.smith@example.com"));

    // Verify normalized email was saved
    assertThat(accountRepository.findByEmail("jane.smith@example.com")).isPresent();
  }

  @Test
  @DisplayName("Should return conflict when email already exists")
  void shouldReturnConflictWhenEmailAlreadyExists() throws Exception {
    // Given - Register first account
    AccountRegistrationRequest firstRequest =
        AccountRegistrationRequest.builder()
            .firstName("Nicole")
            .lastName("Smith")
            .email("smith.duplicate@example.com")
            .password("StrongPassword123")
            .build();

    mockMvc.perform(
        post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(firstRequest)));

    // When & Then - Try to register with same email
    AccountRegistrationRequest duplicateRequest =
        AccountRegistrationRequest.builder()
            .firstName("Jane")
            .lastName("Smith")
            .email("smith.duplicate@example.com")
            .password("VeryStrongPassword456")
            .build();

    mockMvc
        .perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").isArray())
        .andExpect(
            jsonPath("$.message[0]").value("Email already exists: smith.duplicate@example.com"));
  }

  @Test
  @DisplayName("Should return bad request when first name is missing")
  void shouldReturnBadRequestWhenFirstNameIsMissing() throws Exception {
    // Given
    AccountRegistrationRequest request =
        AccountRegistrationRequest.builder()
            .firstName("")
            .lastName("Smith")
            .email("nicole.smith@example.com")
            .password("StrongPassword1234")
            .build();

    // When & Then
    mockMvc
        .perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").isArray());
  }

  @Test
  @DisplayName("Should return bad request when email is invalid")
  void shouldReturnBadRequestWhenEmailIsInvalid() throws Exception {
    // Given
    AccountRegistrationRequest request =
        AccountRegistrationRequest.builder()
            .firstName("Nicole")
            .lastName("Smith")
            .email("invalid-email")
            .password("StrongPassword1234")
            .build();

    // When & Then
    mockMvc
        .perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").isArray());
  }

  @Test
  @DisplayName("Should return bad request when password is too short")
  void shouldReturnBadRequestWhenPasswordIsTooShort() throws Exception {
    // Given
    AccountRegistrationRequest request =
        AccountRegistrationRequest.builder()
            .firstName("Nicole")
            .lastName("Smith")
            .email("nicole.smith@example.com")
            .password("short")
            .build();

    // When & Then
    mockMvc
        .perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").isArray());
  }

  @Test
  @DisplayName("Should login successfully with valid credentials")
  void shouldLoginSuccessfullyWithValidCredentials() throws Exception {
    // Given - Register an account first
    AccountRegistrationRequest registerRequest =
        AccountRegistrationRequest.builder()
            .firstName("Nicole")
            .lastName("Smith")
            .email("nicole.smith@example.com")
            .password("StrongPassword1234")
            .build();

    mockMvc.perform(
        post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(registerRequest)));

    // When - Login with same credentials
    LoginRequest loginRequest =
        LoginRequest.builder()
            .email("nicole.smith@example.com")
            .password("StrongPassword1234")
            .build();

    // Then
    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").exists())
        .andExpect(jsonPath("$.email").value("nicole.smith@example.com"))
        .andExpect(jsonPath("$.firstName").value("Nicole"))
        .andExpect(jsonPath("$.lastName").value("Smith"));
  }

  @Test
  @DisplayName("Should normalize email during login")
  void shouldNormalizeEmailDuringLogin() throws Exception {
    // Given - Register with lowercase email
    AccountRegistrationRequest registerRequest =
        AccountRegistrationRequest.builder()
            .firstName("Nicole")
            .lastName("Smith")
            .email("nicole.smith@example.com")
            .password("StrongPassword1234")
            .build();

    mockMvc.perform(
        post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(registerRequest)));

    // When - Login with uppercase email
    LoginRequest loginRequest =
        LoginRequest.builder()
            .email("NICOLE.SMITH@EXAMPLE.COM")
            .password("StrongPassword1234")
            .build();

    // Then
    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").exists());
  }

  @Test
  @DisplayName("Should return unauthorized when credentials are invalid")
  void shouldReturnUnauthorizedWhenCredentialsAreInvalid() throws Exception {
    // Given - Register an account
    AccountRegistrationRequest registerRequest =
        AccountRegistrationRequest.builder()
            .firstName("Nicole")
            .lastName("Smith")
            .email("nicole.smith@example.com")
            .password("StrongPassword1234")
            .build();

    mockMvc.perform(
        post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(registerRequest)));

    // When - Try to login with wrong password
    LoginRequest loginRequest =
        LoginRequest.builder()
            .email("nicole.smith@example.com")
            .password("WrongPassword1234")
            .build();

    // Then
    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return bad request when login email is missing")
  void shouldReturnBadRequestWhenLoginEmailIsMissing() throws Exception {
    // Given
    LoginRequest loginRequest =
        LoginRequest.builder().email("").password("StrongPassword1234").build();

    // When & Then
    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").isArray());
  }
}
