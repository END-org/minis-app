package com.end.minisbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class MinisBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(MinisBackendApplication.class, args);
  }
}
