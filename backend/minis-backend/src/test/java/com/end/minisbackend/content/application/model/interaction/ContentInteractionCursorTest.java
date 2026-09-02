package com.end.minisbackend.content.application.model.interaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContentInteractionCursorTest {
  @Test
  void cursorRoundTripsRelationshipCreatedAtAndContentId() {
    ContentInteractionCursor cursor =
        new ContentInteractionCursor(
            Instant.parse("2026-07-18T12:00:00Z"),
            UUID.fromString("11111111-1111-1111-1111-111111111111"));

    ContentInteractionCursor decoded = ContentInteractionCursor.decode(cursor.encode());

    assertThat(decoded).isEqualTo(cursor);
  }

  @Test
  void malformedCursorIsRejectedSafely() {
    assertThatThrownBy(() -> ContentInteractionCursor.decode("not-a-cursor"))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.errorCode()).isEqualTo(ApiErrorCode.VALIDATION_ERROR));
  }

  @Test
  void oversizedCursorIsRejectedSafely() {
    assertThatThrownBy(() -> ContentInteractionCursor.decode("a".repeat(300)))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.errorCode()).isEqualTo(ApiErrorCode.VALIDATION_ERROR));
  }
}
