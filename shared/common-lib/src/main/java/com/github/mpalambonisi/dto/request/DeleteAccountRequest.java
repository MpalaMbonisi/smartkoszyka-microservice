package com.github.mpalambonisi.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO for deleting account. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeleteAccountRequest {

  @NotEmpty(message = "Email cannot be empty.")
  @NotBlank(message = "Email cannot be blank.")
  @Email(
      message = "Please provide a valid email address.",
      regexp = "[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,3}",
      flags = Pattern.Flag.CASE_INSENSITIVE)
  private String email;
}
