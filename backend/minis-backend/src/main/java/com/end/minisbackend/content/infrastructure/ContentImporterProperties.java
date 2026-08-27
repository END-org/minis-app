package com.end.minisbackend.content.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "minis.content.importer")
record ContentImporterProperties(
    boolean enabled,
    String manifestPath,
    String assetsRoot,
    String ffprobePath,
    Duration probeTimeout,
    int maxProbeOutputBytes) {
  private static final Duration DEFAULT_PROBE_TIMEOUT = Duration.ofSeconds(10);

  ContentImporterProperties {
    manifestPath = normalize(manifestPath);
    assetsRoot = normalize(assetsRoot);
    ffprobePath = StringUtils.hasText(ffprobePath) ? ffprobePath.trim() : "ffprobe";
    probeTimeout = probeTimeout == null ? DEFAULT_PROBE_TIMEOUT : probeTimeout;
    maxProbeOutputBytes = maxProbeOutputBytes <= 0 ? 1_048_576 : maxProbeOutputBytes;
  }

  void requireImporterConfigured() {
    if (!StringUtils.hasText(manifestPath) || !StringUtils.hasText(assetsRoot)) {
      throw new IllegalStateException(
          "MINIS curated importer manifest and assets root are required");
    }
  }

  private static String normalize(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
