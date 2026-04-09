package com.github.mpalambonisi.exception;

/**
 * Thrown when a registration attempt uses an e-mail address that already exists in the system. Maps
 * to HTTP 409 Conflict via {@link GlobalExceptionHandler}.
 */
public class EmailAlreadyExistsException extends RuntimeException {

  public EmailAlreadyExistsException(String message) {
    super(message);
  }
}
