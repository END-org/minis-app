package com.end.minisbackend.content.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.end.minisbackend.content.application.port.ContentStorage;
import com.end.minisbackend.content.application.port.ContentStorageException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;

class ContentStorageHealthIndicatorTest {
  @Test
  void availableStorageReportsUpWithoutInfrastructureDetails() {
    ContentStorage storage = mock(ContentStorage.class);

    var health = new ContentStorageHealthIndicator(storage).health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails()).isEmpty();
  }

  @Test
  void unavailableStorageReportsDownWithoutLeakingFailureDetails() {
    ContentStorage storage = mock(ContentStorage.class);
    doThrow(new ContentStorageException("endpoint and private detail"))
        .when(storage)
        .verifyAvailable();

    var health = new ContentStorageHealthIndicator(storage).health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).isEmpty();
  }
}
