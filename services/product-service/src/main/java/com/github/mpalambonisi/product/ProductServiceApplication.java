package com.github.mpalambonisi.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Entry point for the product-service. */
@SpringBootApplication
@EnableScheduling // Enable scheduling for the scraper task
@ComponentScan(basePackages = {"com.github.mpalambonisi.product", "com.github.mpalambonisi.common"})
public class ProductServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(ProductServiceApplication.class, args);
  }
}
