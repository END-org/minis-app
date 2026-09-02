package com.end.minisbackend.content.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ContentStoragePropertiesTest {
  @Test
  void validConfigurationUsesBoundedTimeoutDefaults() {
    ContentStorageProperties properties = properties(Duration.ofMinutes(10), null, null, null);

    properties.requireConfigured();

    assertThat(properties.signedReadLifetimeSeconds()).isEqualTo(600);
    assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(5));
    assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(30));
    assertThat(properties.writeTimeout()).isEqualTo(Duration.ofMinutes(2));
  }

  @Test
  void malformedEndpointFailsBeforeStorageRequest() {
    ContentStorageProperties properties =
        new ContentStorageProperties(
            "not-a-url",
            "http://localhost:9000",
            "access",
            "secret",
            "bucket",
            Duration.ofMinutes(10),
            Duration.ofSeconds(5),
            Duration.ofSeconds(30),
            Duration.ofMinutes(2),
            false);

    assertThatThrownBy(properties::requireConfigured)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("internal endpoint");
  }

  @Test
  void invalidLifetimeAndTimeoutFailInsteadOfBeingSilentlyClamped() {
    ContentStorageProperties invalidLifetime =
        properties(Duration.ofDays(8), Duration.ofSeconds(5), null, null);
    ContentStorageProperties invalidTimeout =
        properties(Duration.ofMinutes(10), Duration.ZERO, null, null);
    ContentStorageProperties roundedLifetime =
        properties(Duration.ofMillis(999), Duration.ofSeconds(5), null, null);
    ContentStorageProperties roundedTimeout =
        properties(Duration.ofMinutes(10), Duration.ofNanos(1), null, null);

    assertThatThrownBy(invalidLifetime::requireConfigured)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("signed read lifetime");
    assertThatThrownBy(invalidTimeout::requireConfigured)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("connect timeout");
    assertThatThrownBy(roundedLifetime::requireConfigured)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("signed read lifetime");
    assertThatThrownBy(roundedTimeout::requireConfigured)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("connect timeout");
  }

  private static ContentStorageProperties properties(
      Duration signedLifetime,
      Duration connectTimeout,
      Duration readTimeout,
      Duration writeTimeout) {
    return new ContentStorageProperties(
        "http://localhost:9000",
        "https://media.example.test",
        "access",
        "secret",
        "bucket",
        signedLifetime,
        connectTimeout,
        readTimeout,
        writeTimeout,
        false);
  }
}
