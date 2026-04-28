package com.github.mpalambonisi.shoppinglist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/** Entry point for the shopping-list-service. */
@SpringBootApplication
@ComponentScan(
    basePackages = {"com.github.mpalambonisi.shoppinglist", "com.github.mpalambonisi.common"})
public class ShoppingListApplication {
  public static void main(String[] args) {
    SpringApplication.run(ShoppingListApplication.class, args);
  }
}
