package com.end.minisbackend.content.infrastructure;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.end.minisbackend.content.application.ContentImportException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

class FfprobeMediaProbeTest {
  @TempDir Path tempDir;

  @Test
  void missingFfprobeFailsClearlyWithoutInventingMetadata() throws Exception {
    Path asset = tempDir.resolve("asset.mp4");
    Files.writeString(asset, "not inspected");
    FfprobeMediaProbe probe =
        new FfprobeMediaProbe(
            new ContentImporterProperties(
                false, null, null, "missing-ffprobe-binary-for-test", Duration.ofMillis(100), 1024),
            new ObjectMapper());

    assertThatThrownBy(() -> probe.inspect(asset)).isInstanceOf(ContentImportException.class);
  }
}
