package com.github.mpalambonisi.exception;

/**
 * Thrown when a requested resource (account, product, list …) cannot be found. Maps to HTTP 404 Not
 * Found via {@link GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException(String message) {
    super(message);
  }
}
