package com.github.mpalambonisi.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/** Entry point for the auth-service. */
@SpringBootApplication
@ComponentScan(basePackages = {"com.github.mpalambonisi.auth", "com.github.mpalambonisi.service"})
public class AuthServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(AuthServiceApplication.class, args);
  }
}
