package com.github.mpalambonisi.gateway.filter;

import com.github.mpalambonisi.common.service.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Gateway security config. JWT validation is handled by JwtAuthenticationGatewayFilter per-route.
 * This config just disables CSRF (stateless API) and permits all requests through the gateway
 * filter handles auth, not Spring Security here.
 */
@Slf4j
@Component
public class JwtAuthenticationGatewayFilter
    extends AbstractGatewayFilterFactory<JwtAuthenticationGatewayFilter.Config> {

  private final JwtService jwtService;

  public JwtAuthenticationGatewayFilter(JwtService jwtService) {
    super(Config.class);
    this.jwtService = jwtService;
  }

  @Override
  public GatewayFilter apply(Config config) {
    return (exchange, chain) -> {
      String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

      // No token — reject immediately
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        log.debug("Request rejected — missing or malformed Authorization header");
        return unauthorised(exchange);
      }

      String token = authHeader.substring(7);

      try {
        String username = jwtService.extractUsername(token);

        if (username == null || !jwtService.isTokenValid(token, username)) {
          log.debug("Request rejected — invalid or expired token");
          return unauthorised(exchange);
        }

        // Propagate the username downstream as a header
        // Downstream services read this instead of re-validating the JWT
        ServerWebExchange mutatedExchange =
            exchange.mutate().request(r -> r.header("X-Authenticated-User", username)).build();

        log.debug("JWT valid for user: {} — forwarding request", username);
        return chain.filter(mutatedExchange);

      } catch (Exception e) {
        log.warn("JWT validation failed: {}", e.getMessage());
        return unauthorised(exchange);
      }
    };
  }

  private Mono<Void> unauthorised(ServerWebExchange exchange) {
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    return exchange.getResponse().setComplete();
  }

  /**
   * Config class required by AbstractGatewayFilterFactory. Add configurable fields here later (e.g.
   * excluded paths).
   */
  public static class Config {}
}
