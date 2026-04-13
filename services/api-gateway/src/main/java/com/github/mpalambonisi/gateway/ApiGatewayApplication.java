package com.github.mpalambonisi.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * API Gateway application entry point. Excludes GlobalExceptionHandler from common-lib as gateway
 * uses GatewayExceptionHandler.
 */
@SpringBootApplication
@ComponentScan(
    basePackages = {"com.github.mpalambonisi.apigateway", "com.github.mpalambonisi.common"},
    excludeFilters =
        @ComponentScan.Filter(
            type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
            classes = com.github.mpalambonisi.common.exception.GlobalExceptionHandler.class))
public class ApiGatewayApplication {

  public static void main(String[] args) {
    SpringApplication.run(ApiGatewayApplication.class, args);
  }
}
