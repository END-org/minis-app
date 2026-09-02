package com.end.minisbackend.content.application.model.feed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContentCursorTest {
  @Test
  void cursorRoundTripsPublishedAtAndId() {
    ContentCursor cursor =
        new ContentCursor(
            Instant.parse("2026-07-17T12:00:00Z"),
            UUID.fromString("11111111-1111-1111-1111-111111111111"));

    ContentCursor decoded = ContentCursor.decode(cursor.encode());

    assertThat(decoded).isEqualTo(cursor);
  }

  @Test
  void malformedCursorIsRejectedSafely() {
    assertThatThrownBy(() -> ContentCursor.decode("not-a-cursor"))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.errorCode()).isEqualTo(ApiErrorCode.VALIDATION_ERROR));
  }

  @Test
  void oversizedCursorIsRejectedSafely() {
    assertThatThrownBy(() -> ContentCursor.decode("a".repeat(300)))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.errorCode()).isEqualTo(ApiErrorCode.VALIDATION_ERROR));
  }
}
