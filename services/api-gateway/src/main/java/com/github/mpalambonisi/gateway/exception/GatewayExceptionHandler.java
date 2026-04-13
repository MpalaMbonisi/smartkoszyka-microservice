package com.github.mpalambonisi.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Reactive error handler for the gateway. Replaces GlobalExceptionHandler which is servlet-only.
 */
@Component
@Order(-1) // run before Spring's default error handler
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

  private final ObjectMapper objectMapper;

  public GatewayExceptionHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);

    try {
      byte[] bytes = objectMapper.writeValueAsBytes(new ErrorBody(List.of(ex.getMessage())));
      DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
      return exchange.getResponse().writeWith(Mono.just(buffer));
    } catch (JsonProcessingException e) {
      return exchange.getResponse().setComplete();
    }
  }

  record ErrorBody(List<String> message) {}
}
