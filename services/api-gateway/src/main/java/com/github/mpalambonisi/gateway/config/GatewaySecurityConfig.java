package com.github.mpalambonisi.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Gateway security config.
 *
 * <p>JWT validation is handled by JwtAuthenticationGatewayFilter per-route. This config just
 * disables CSRF (stateless API) and permits all requests through — the gateway filter handles auth,
 * not Spring Security here.
 */
@Configuration
@EnableWebFluxSecurity // ← WebFlux, not WebSecurity
public class GatewaySecurityConfig {

  /**
   * Configures the security filter chain for reactive web flows.
   *
   * @param http the ServerHttpSecurity to configure
   * @return the built SecurityWebFilterChain
   */
  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(
            exchanges ->
                exchanges
                    .anyExchange()
                    .permitAll() // gateway filter handles JWT, not Spring Security
            )
        .build();
  }
}
