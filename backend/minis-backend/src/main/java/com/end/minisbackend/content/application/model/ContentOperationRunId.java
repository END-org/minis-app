package com.end.minisbackend.content.application.model;

import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

public final class ContentOperationRunId {
  private static final int MAX_LENGTH = 64;
  private static final Pattern SAFE_VALUE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");

  private ContentOperationRunId() {}

  public static String normalize(String value) {
    if (!StringUtils.hasText(value)) {
      return UUID.randomUUID().toString();
    }
    String normalized = value.trim();
    if (normalized.length() > MAX_LENGTH || !SAFE_VALUE.matcher(normalized).matches()) {
      throw new IllegalArgumentException("content operation run ID is invalid");
    }
    return normalized;
  }
}
