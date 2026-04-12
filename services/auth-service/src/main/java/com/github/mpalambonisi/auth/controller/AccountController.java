package com.github.mpalambonisi.auth.controller;

import com.github.mpalambonisi.auth.service.AccountService;
import com.github.mpalambonisi.dto.request.DeleteAccountRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for account operations (deleting accounts). */
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

  private final AccountService accountService;

  /**
   * Delete account.
   *
   * @param request The email of the user
   * @param authentication The authenticated user
   * @return No Content Response
   */
  @DeleteMapping("/delete")
  public ResponseEntity<Void> deleteAccount(
      @Valid @RequestBody DeleteAccountRequest request, Authentication authentication) {

    // Security Check: Ensure user is only deleting themselves
    String currentAuthenticatedEmail = authentication.getName();
    if (!currentAuthenticatedEmail.equalsIgnoreCase(request.getEmail().trim())) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    accountService.deleteAccount(request.getEmail());

    return ResponseEntity.noContent().build();
  }
}
