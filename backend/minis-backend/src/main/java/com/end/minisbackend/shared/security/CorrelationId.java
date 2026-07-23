package com.end.minisbackend.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import java.util.regex.Pattern;

/** Correlation ID header and request attribute contract. */
public final class CorrelationId {
  public static final String HEADER_NAME = "X-Correlation-ID";
  public static final String ATTRIBUTE_NAME = CorrelationId.class.getName() + ".value";

  private static final int MAX_LENGTH = 64;
  private static final Pattern ALLOWED_VALUE = Pattern.compile("[A-Za-z0-9._-]{1,64}");

  private CorrelationId() {}

  public static String fromRequest(HttpServletRequest request) {
    Object attribute = request.getAttribute(ATTRIBUTE_NAME);
    if (attribute instanceof String correlationId && isValid(correlationId)) {
      return correlationId;
    }
    String correlationId = normalize(request.getHeader(HEADER_NAME));
    request.setAttribute(ATTRIBUTE_NAME, correlationId);
    return correlationId;
  }

  public static String normalize(String candidate) {
    if (candidate == null) {
      return generate();
    }
    String trimmed = candidate.trim();
    if (isValid(trimmed)) {
      return trimmed;
    }
    return generate();
  }

  static boolean isValid(String value) {
    return value != null && value.length() <= MAX_LENGTH && ALLOWED_VALUE.matcher(value).matches();
  }

  private static String generate() {
    return UUID.randomUUID().toString();
  }
}
