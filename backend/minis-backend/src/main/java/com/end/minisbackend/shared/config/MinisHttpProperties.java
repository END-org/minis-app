package com.end.minisbackend.shared.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "minis.http")
public record MinisHttpProperties(List<String> allowedOrigins) {
  public MinisHttpProperties {
    allowedOrigins =
        allowedOrigins == null
            ? List.of()
            : List.copyOf(
                allowedOrigins.stream().filter(StringUtils::hasText).map(String::trim).toList());
    if (allowedOrigins.stream().anyMatch("*"::equals)) {
      throw new IllegalArgumentException("minis.http.allowed-origins must not contain '*'");
    }
  }

  @Override
  public List<String> allowedOrigins() {
    return List.copyOf(allowedOrigins);
  }

  public boolean hasAllowedOrigins() {
    return !allowedOrigins.isEmpty();
  }
}
