package com.github.mpalambonisi.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Returned by the auth-service after successful login or registration. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

  private String token;
  private String email;
  private String firstName;
  private String lastName;
}
