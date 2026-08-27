package com.end.minisbackend.content.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "minis.content.storage")
record ContentStorageProperties(
    String internalEndpoint,
    String publicEndpoint,
    String accessKey,
    String secretKey,
    String bucket,
    Duration signedReadLifetime,
    boolean autoCreateBucket) {
  private static final Duration DEFAULT_SIGNED_READ_LIFETIME = Duration.ofMinutes(10);

  ContentStorageProperties {
    internalEndpoint = normalize(internalEndpoint);
    publicEndpoint = normalize(publicEndpoint);
    accessKey = normalize(accessKey);
    secretKey = normalize(secretKey);
    bucket = StringUtils.hasText(bucket) ? bucket.trim() : "minis-event-media";
    signedReadLifetime =
        signedReadLifetime == null ? DEFAULT_SIGNED_READ_LIFETIME : signedReadLifetime;
  }

  void requireConfigured() {
    if (!StringUtils.hasText(internalEndpoint)
        || !StringUtils.hasText(publicEndpoint)
        || !StringUtils.hasText(accessKey)
        || !StringUtils.hasText(secretKey)
        || !StringUtils.hasText(bucket)) {
      throw new IllegalStateException("MINIS content storage is not fully configured");
    }
  }

  int signedReadLifetimeSeconds() {
    long seconds = Math.max(1L, signedReadLifetime.toSeconds());
    return Math.toIntExact(Math.min(seconds, 604_800L));
  }

  private static String normalize(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
