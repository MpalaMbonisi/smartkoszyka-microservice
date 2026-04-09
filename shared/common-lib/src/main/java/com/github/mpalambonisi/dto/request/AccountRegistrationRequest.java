package com.github.mpalambonisi.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request body for the {@code POST /auth/register} endpoint. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountRegistrationRequest {

  @NotEmpty(message = "First name cannot be empty.")
  @NotBlank(message = "First name cannot be blank.")
  private String firstName;

  @NotEmpty(message = "Last name cannot be empty.")
  @NotBlank(message = "Last name cannot be blank.")
  private String lastName;

  @NotEmpty(message = "Email cannot be empty.")
  @NotBlank(message = "Email cannot be blank.")
  @Email(
      message = "Please provide a valid email address.",
      regexp = "[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,3}",
      flags = Pattern.Flag.CASE_INSENSITIVE)
  private String email;

  @NotEmpty(message = "Password cannot be empty.")
  @NotBlank(message = "Password cannot be blank.")
  @Size(min = 8, message = "Password must be at least 8 characters long.")
  private String password;
}
