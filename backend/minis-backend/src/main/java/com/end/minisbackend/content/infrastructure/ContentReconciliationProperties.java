package com.end.minisbackend.content.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "minis.content.reconciliation")
record ContentReconciliationProperties(boolean enabled, Duration staleAge) {
  private static final Duration DEFAULT_STALE_AGE = Duration.ofMinutes(30);

  ContentReconciliationProperties {
    staleAge = staleAge == null ? DEFAULT_STALE_AGE : staleAge;
  }
}
