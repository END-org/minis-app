package com.end.minisbackend.content.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "minis.content.importer")
public record ContentImporterProperties(
    boolean enabled,
    String manifestPath,
    String assetsRoot,
    String ffprobePath,
    Duration probeTimeout,
    int maxProbeOutputBytes) {
  private static final Duration DEFAULT_PROBE_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration MAX_PROBE_TIMEOUT = Duration.ofMinutes(1);
  private static final int MAX_PROBE_OUTPUT_BYTES = 4 * 1024 * 1024;

  public ContentImporterProperties {
    manifestPath = normalize(manifestPath);
    assetsRoot = normalize(assetsRoot);
    ffprobePath = StringUtils.hasText(ffprobePath) ? ffprobePath.trim() : "ffprobe";
    probeTimeout = probeTimeout == null ? DEFAULT_PROBE_TIMEOUT : probeTimeout;
    maxProbeOutputBytes = maxProbeOutputBytes <= 0 ? 1_048_576 : maxProbeOutputBytes;
  }

  public void requireImporterConfigured() {
    if (!StringUtils.hasText(manifestPath) || !StringUtils.hasText(assetsRoot)) {
      throw new IllegalStateException(
          "MINIS curated importer manifest and assets root are required");
    }
    requireProbeConfigured();
  }

  public void requireProbeConfigured() {
    if (probeTimeout.isZero()
        || probeTimeout.isNegative()
        || probeTimeout.compareTo(MAX_PROBE_TIMEOUT) > 0) {
      throw new IllegalStateException("MINIS ffprobe timeout must be between zero and one minute");
    }
    if (maxProbeOutputBytes <= 0 || maxProbeOutputBytes > MAX_PROBE_OUTPUT_BYTES) {
      throw new IllegalStateException(
          "MINIS ffprobe output limit must be between 1 byte and 4 MiB");
    }
  }

  private static String normalize(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
