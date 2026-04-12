package com.github.mpalambonisi.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mpalambonisi.common.exception.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/** Custom entry point for authentication errors. */
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

  @SuppressWarnings("EI_EXPOSE_REP2")
  private final ObjectMapper objectMapper;

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException, ServletException {

    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json");

    ErrorResponse errorResponse =
        new ErrorResponse(List.of("User is unauthorised! Authentication Failed!"));

    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
  }
}
