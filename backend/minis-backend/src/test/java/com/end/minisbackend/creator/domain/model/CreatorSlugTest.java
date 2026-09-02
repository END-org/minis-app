package com.end.minisbackend.creator.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CreatorSlugTest {
  @Test
  void normalizesCreatorSlug() {
    assertThat(CreatorSlug.normalize("  Event Creator! 2026 ")).isEqualTo("event-creator-2026");
  }

  @Test
  void rejectsBlankSlug() {
    assertThatThrownBy(() -> CreatorSlug.normalize("   "))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
