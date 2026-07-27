package com.end.minisbackend.shared.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class MinisHttpPropertiesTest {
  @Test
  void allowedOriginsNormalizeWhitespaceAndIgnoreEmptyValues() {
    MinisHttpProperties properties =
        new MinisHttpProperties(List.of(" http://localhost:5173 ", "", "   "));

    assertThat(properties.allowedOrigins()).containsExactly("http://localhost:5173");
    assertThat(properties.hasAllowedOrigins()).isTrue();
  }

  @Test
  void emptyAllowedOriginsAreHandledSafely() {
    MinisHttpProperties properties = new MinisHttpProperties(List.of("", " "));

    assertThat(properties.allowedOrigins()).isEmpty();
    assertThat(properties.hasAllowedOrigins()).isFalse();
  }

  @Test
  void wildcardOriginsAreRejected() {
    assertThatThrownBy(() -> new MinisHttpProperties(List.of("*")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must not contain '*'");
  }
}
