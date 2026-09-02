package com.end.minisbackend.content.infrastructure.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "minis.content.storage")
public record ContentStorageProperties(
    String internalEndpoint,
    String publicEndpoint,
    String accessKey,
    String secretKey,
    String bucket,
    Duration signedReadLifetime,
    Duration connectTimeout,
    Duration readTimeout,
    Duration writeTimeout,
    boolean autoCreateBucket) {
  private static final Duration DEFAULT_SIGNED_READ_LIFETIME = Duration.ofMinutes(10);
  private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);
  private static final Duration DEFAULT_WRITE_TIMEOUT = Duration.ofMinutes(2);
  private static final Duration MAX_SIGNED_READ_LIFETIME = Duration.ofDays(7);
  private static final Duration MAX_HTTP_TIMEOUT = Duration.ofMinutes(10);

  public ContentStorageProperties {
    internalEndpoint = normalize(internalEndpoint);
    publicEndpoint = normalize(publicEndpoint);
    accessKey = normalize(accessKey);
    secretKey = normalize(secretKey);
    bucket = StringUtils.hasText(bucket) ? bucket.trim() : "minis-event-media";
    signedReadLifetime =
        signedReadLifetime == null ? DEFAULT_SIGNED_READ_LIFETIME : signedReadLifetime;
    connectTimeout = connectTimeout == null ? DEFAULT_CONNECT_TIMEOUT : connectTimeout;
    readTimeout = readTimeout == null ? DEFAULT_READ_TIMEOUT : readTimeout;
    writeTimeout = writeTimeout == null ? DEFAULT_WRITE_TIMEOUT : writeTimeout;
  }

  public void requireConfigured() {
    if (!StringUtils.hasText(internalEndpoint)
        || !StringUtils.hasText(publicEndpoint)
        || !StringUtils.hasText(accessKey)
        || !StringUtils.hasText(secretKey)
        || !StringUtils.hasText(bucket)) {
      throw new IllegalStateException("MINIS content storage is not fully configured");
    }
    requireHttpEndpoint("internal endpoint", internalEndpoint);
    requireHttpEndpoint("public endpoint", publicEndpoint);
    requireDuration(
        "signed read lifetime",
        signedReadLifetime,
        Duration.ofSeconds(1),
        MAX_SIGNED_READ_LIFETIME);
    requireDuration("connect timeout", connectTimeout, Duration.ofMillis(1), MAX_HTTP_TIMEOUT);
    requireDuration("read timeout", readTimeout, Duration.ofMillis(1), MAX_HTTP_TIMEOUT);
    requireDuration("write timeout", writeTimeout, Duration.ofMillis(1), MAX_HTTP_TIMEOUT);
  }

  public int signedReadLifetimeSeconds() {
    return Math.toIntExact(signedReadLifetime.toSeconds());
  }

  private static void requireHttpEndpoint(String name, String value) {
    try {
      URI endpoint = new URI(value);
      if (!("http".equalsIgnoreCase(endpoint.getScheme())
              || "https".equalsIgnoreCase(endpoint.getScheme()))
          || !StringUtils.hasText(endpoint.getHost())) {
        throw new IllegalStateException("MINIS content storage " + name + " must be an HTTP URL");
      }
    } catch (URISyntaxException exception) {
      throw new IllegalStateException(
          "MINIS content storage " + name + " must be an HTTP URL", exception);
    }
  }

  private static void requireDuration(
      String name, Duration value, Duration minimum, Duration maximum) {
    if (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
      throw new IllegalStateException(
          "MINIS content storage "
              + name
              + " must be at least "
              + minimum
              + " and at most "
              + maximum);
    }
  }

  private static String normalize(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
