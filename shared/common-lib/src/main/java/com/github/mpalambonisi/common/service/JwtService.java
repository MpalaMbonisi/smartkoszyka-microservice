package com.github.mpalambonisi.common.service;

import io.jsonwebtoken.Claims;
import java.util.function.Function;
import org.springframework.security.core.userdetails.UserDetails;

/** Contract for JWT operations shared across all microservices. */
public interface JwtService {

  /**
   * Generate a signed JWT for the given user.
   *
   * @param user Spring Security {@link UserDetails} whose username becomes the subject
   * @return compact serialized JWT string
   */
  String generateToken(UserDetails user);

  /**
   * Extract the {@code sub} claim (username / email) from a token.
   *
   * @param token compact JWT string
   * @return the subject claim value
   */
  String extractUsername(String token);

  /**
   * Validate that the token belongs to {@code userDetails} and has not expired.
   *
   * @param token compact JWT string
   * @param userDetails the principal to check against
   * @return {@code true} if the token is valid
   */
  boolean isTokenValid(String token, UserDetails userDetails);

  /**
   * Generic claim extractor.
   *
   * @param token compact JWT string
   * @param claimResolver function applied to the full {@link Claims} object
   * @param <T> return type of the resolver
   * @return resolved claim value
   */
  <T> T extractClaim(String token, Function<Claims, T> claimResolver);
}
