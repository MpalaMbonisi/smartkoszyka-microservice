package com.github.mpalambonisi.common.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

/** Standardised error envelope returned by all microservices. */
@Getter
public class ErrorResponse {

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
  private final LocalDateTime timestamp;

  private final List<String> message;

  public ErrorResponse(List<String> message) {
    this.timestamp = LocalDateTime.now();
    this.message = (message != null) ? new ArrayList<>(message) : List.of();
  }

  /** Defensive copy — prevents callers from mutating the internal list. */
  public List<String> getMessage() {
    return new ArrayList<>(this.message);
  }
}
