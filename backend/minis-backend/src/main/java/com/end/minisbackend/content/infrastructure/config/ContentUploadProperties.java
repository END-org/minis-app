package com.end.minisbackend.content.infrastructure.config;

import com.end.minisbackend.content.application.port.ContentUploadSettings;
import com.end.minisbackend.content.domain.policy.EventMediaProfilePolicy;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DataSizeUnit;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

@ConfigurationProperties(prefix = "minis.content.upload")
record ContentUploadProperties(
    @DataSizeUnit(DataUnit.BYTES) DataSize maxFileSize, Path tempDirectory, Duration staleAge)
    implements ContentUploadSettings {
  private static final DataSize DEFAULT_MAX_FILE_SIZE =
      DataSize.ofBytes(EventMediaProfilePolicy.MAX_SIZE_BYTES);
  private static final Duration DEFAULT_STALE_AGE = Duration.ofMinutes(30);

  ContentUploadProperties {
    maxFileSize = maxFileSize == null ? DEFAULT_MAX_FILE_SIZE : maxFileSize;
    tempDirectory =
        tempDirectory == null
            ? Path.of(System.getProperty("java.io.tmpdir"), "minis-content-uploads")
            : tempDirectory;
    staleAge = staleAge == null ? DEFAULT_STALE_AGE : staleAge;
    if (maxFileSize.toBytes() <= 0L
        || maxFileSize.toBytes() > EventMediaProfilePolicy.MAX_SIZE_BYTES) {
      throw new IllegalStateException(
          "MINIS upload max file size must be between 1 byte and 200 MB");
    }
    if (staleAge.isZero() || staleAge.isNegative() || staleAge.compareTo(Duration.ofDays(1)) > 0) {
      throw new IllegalStateException("MINIS stale upload age must be between zero and one day");
    }
  }

  @Override
  public long maxFileSizeBytes() {
    return maxFileSize.toBytes();
  }

  @Override
  public Duration staleUploadAge() {
    return staleAge;
  }
}
