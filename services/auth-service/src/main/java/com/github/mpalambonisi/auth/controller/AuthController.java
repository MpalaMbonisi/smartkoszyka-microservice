package com.github.mpalambonisi.auth.controller;

import com.github.mpalambonisi.auth.model.Account;
import com.github.mpalambonisi.auth.service.AccountService;
import com.github.mpalambonisi.dto.request.AccountRegistrationRequest;
import com.github.mpalambonisi.dto.request.LoginRequest;
import com.github.mpalambonisi.dto.response.AuthResponse;
import com.github.mpalambonisi.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for authentication operations (login and registration). */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AccountService accountService;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;
  private final UserDetailsService userDetailsService;

  /**
   * Register a new user account.
   *
   * @param request The registration request containing user details
   * @return AuthResponse with JWT token and user information
   */
  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(
      @Valid @RequestBody AccountRegistrationRequest request) {

    // Register the account
    Account account =
        accountService.registerAccount(
            request.getEmail(),
            request.getPassword(),
            request.getFirstName(),
            request.getLastName());

    // Load user details for token generation
    UserDetails userDetails = userDetailsService.loadUserByUsername(account.getEmail());

    // Generate JWT token
    String token = jwtService.generateToken(userDetails);

    // Build response
    AuthResponse response =
        AuthResponse.builder()
            .token(token)
            .email(account.getEmail())
            .firstName(account.getFirstName())
            .lastName(account.getLastName())
            .build();

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Authenticate user and generate JWT token.
   *
   * @param request The login request containing credentials
   * @return AuthResponse with JWT token and user information
   */
  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    // Authenticate user
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            request.getEmail().toLowerCase().trim(), request.getPassword()));

    // Load user details
    Account account = accountService.getAccountByEmail(request.getEmail());
    UserDetails userDetails = userDetailsService.loadUserByUsername(account.getEmail());

    // Generate JWT token
    String token = jwtService.generateToken(userDetails);

    // Build response
    AuthResponse response =
        AuthResponse.builder()
            .token(token)
            .email(account.getEmail())
            .firstName(account.getFirstName())
            .lastName(account.getLastName())
            .build();

    return ResponseEntity.ok(response);
  }
}
