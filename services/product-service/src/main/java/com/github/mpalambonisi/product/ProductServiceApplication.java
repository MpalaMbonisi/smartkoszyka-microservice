package com.github.mpalambonisi.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/** Entry point for the product-service. */
@SpringBootApplication
@ComponentScan(
    basePackages = {"com.github.mpalambonisi.product", "com.github.mpalambonisi.service"})
public class ProductServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(ProductServiceApplication.class, args);
  }
}
