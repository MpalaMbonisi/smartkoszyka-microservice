package com.github.mpalambonisi.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * API Gateway application entry point. Excludes GlobalExceptionHandler from common-lib as gateway
 * uses GatewayExceptionHandler.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.github.mpalambonisi.gateway", "com.github.mpalambonisi.common"})
public class ApiGatewayApplication {

  public static void main(String[] args) {
    SpringApplication.run(ApiGatewayApplication.class, args);
  }
}
