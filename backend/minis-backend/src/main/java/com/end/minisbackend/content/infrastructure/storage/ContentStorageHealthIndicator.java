package com.end.minisbackend.content.infrastructure.storage;

import com.end.minisbackend.content.application.port.ContentStorage;
import com.end.minisbackend.content.application.port.ContentStorageException;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
class ContentStorageHealthIndicator implements HealthIndicator {
  private final ContentStorage contentStorage;

  ContentStorageHealthIndicator(ContentStorage contentStorage) {
    this.contentStorage = contentStorage;
  }

  @Override
  public Health health() {
    try {
      contentStorage.verifyAvailable();
      return Health.up().build();
    } catch (ContentStorageException exception) {
      return Health.down().build();
    }
  }
}
