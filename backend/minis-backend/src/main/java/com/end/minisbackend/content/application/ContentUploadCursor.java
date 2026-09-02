package com.end.minisbackend.content.application;

import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public record ContentUploadCursor(Instant createdAt, UUID contentId) {
  private static final String VERSION = "v1";
  private static final int MAX_CURSOR_LENGTH = 256;

  public String encode() {
    String raw = VERSION + "|" + createdAt + "|" + contentId;
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
  }

  public static ContentUploadCursor decode(String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return null;
    }
    if (cursor.length() > MAX_CURSOR_LENGTH) {
      throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
    }
    try {
      String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
      String[] parts = raw.split("\\|", -1);
      if (parts.length != 3 || !VERSION.equals(parts[0])) {
        throw new IllegalArgumentException("unsupported cursor");
      }
      return new ContentUploadCursor(Instant.parse(parts[1]), UUID.fromString(parts[2]));
    } catch (IllegalArgumentException exception) {
      throw new ApiException(ApiErrorCode.VALIDATION_ERROR, exception);
    }
  }
}
