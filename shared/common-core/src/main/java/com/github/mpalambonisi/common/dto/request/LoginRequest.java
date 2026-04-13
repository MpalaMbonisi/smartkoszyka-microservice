package com.github.mpalambonisi.common.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request body for the {@code POST /auth/login} endpoint. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {

  @NotEmpty(message = "Email cannot be empty.")
  @NotBlank(message = "Email cannot be blank.")
  private String email;

  @NotEmpty(message = "Password cannot be empty.")
  @NotBlank(message = "Password cannot be blank.")
  @Size(min = 8, message = "Password must be at least 8 characters long.")
  private String password;
}
