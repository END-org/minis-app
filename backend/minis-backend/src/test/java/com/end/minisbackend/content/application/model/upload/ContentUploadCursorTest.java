package com.end.minisbackend.content.application.model.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.end.minisbackend.shared.error.ApiException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContentUploadCursorTest {
  @Test
  void cursorRoundTripPreservesCreatedAtAndContentId() {
    ContentUploadCursor cursor =
        new ContentUploadCursor(
            Instant.parse("2026-07-18T12:00:00Z"),
            UUID.fromString("11111111-1111-1111-1111-111111111111"));

    ContentUploadCursor decoded = ContentUploadCursor.decode(cursor.encode());

    assertThat(decoded.createdAt()).isEqualTo(cursor.createdAt());
    assertThat(decoded.contentId()).isEqualTo(cursor.contentId());
  }

  @Test
  void blankCursorMeansFirstPage() {
    assertThat(ContentUploadCursor.decode(null)).isNull();
    assertThat(ContentUploadCursor.decode(" ")).isNull();
  }

  @Test
  void malformedCursorIsValidationError() {
    assertThatThrownBy(() -> ContentUploadCursor.decode("not-a-real-cursor"))
        .isInstanceOf(ApiException.class)
        .satisfies(
            throwable ->
                assertThat(((ApiException) throwable).errorCode().name())
                    .isEqualTo("VALIDATION_ERROR"));
  }
}
