package com.end.minisbackend.content.application.model.interaction;

import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public record ContentInteractionCursor(Instant relationshipCreatedAt, UUID contentId) {
  private static final String VERSION = "v1";
  private static final int MAX_ENCODED_LENGTH = 256;

  public String encode() {
    String payload = VERSION + "|" + relationshipCreatedAt + "|" + contentId;
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
  }

  public static ContentInteractionCursor decode(String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return null;
    }
    if (cursor.length() > MAX_ENCODED_LENGTH) {
      throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
    }
    try {
      String payload =
          new String(Base64.getUrlDecoder().decode(cursor.trim()), StandardCharsets.UTF_8);
      String[] parts = payload.split("\\|", -1);
      if (parts.length != 3 || !VERSION.equals(parts[0])) {
        throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
      }
      return new ContentInteractionCursor(Instant.parse(parts[1]), UUID.fromString(parts[2]));
    } catch (DateTimeException | IllegalArgumentException exception) {
      throw new ApiException(ApiErrorCode.VALIDATION_ERROR, exception);
    }
  }
}
