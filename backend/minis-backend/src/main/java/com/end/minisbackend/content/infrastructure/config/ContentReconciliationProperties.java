package com.end.minisbackend.content.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "minis.content.reconciliation")
public record ContentReconciliationProperties(boolean enabled, Duration staleAge) {
  private static final Duration DEFAULT_STALE_AGE = Duration.ofMinutes(30);

  public ContentReconciliationProperties {
    staleAge = staleAge == null ? DEFAULT_STALE_AGE : staleAge;
  }
}
