package com.end.minisbackend.content.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.end.minisbackend.content.application.ContentImportException;
import com.end.minisbackend.content.application.MediaInspection;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class FfprobeMediaProbeTest {
  private static final Path ASSET = Path.of("asset.mp4");

  @Test
  void validProbeOutputCreatesDetectedMediaInspection() throws Exception {
    Process process = completedProcess(validOutput(), "", 0);

    MediaInspection inspection = probe(process, Duration.ofSeconds(1), 4096).inspect(ASSET);

    assertThat(inspection.container()).contains("mp4");
    assertThat(inspection.videoCodec()).isEqualTo("h264");
    assertThat(inspection.audioCodec()).isEqualTo("aac");
    assertThat(inspection.durationMs()).isEqualTo(60_000L);
    assertThat(inspection.width()).isEqualTo(1080);
    assertThat(inspection.height()).isEqualTo(1920);
    assertThat(inspection.frameRate()).isEqualTo(30d);
  }

  @Test
  void missingFfprobeFailsClearlyWithoutInventingMetadata() throws Exception {
    FfprobeProcessLauncher launcher = mock(FfprobeProcessLauncher.class);
    when(launcher.start(anyString(), any(Path.class))).thenThrow(new IOException("missing"));

    assertThatThrownBy(() -> probe(launcher, Duration.ofMillis(100), 1024).inspect(ASSET))
        .isInstanceOf(ContentImportException.class)
        .hasMessageContaining("not available");
  }

  @Test
  void malformedProbeOutputIsRejected() throws Exception {
    Process process = completedProcess("not-json", "", 0);

    assertThatThrownBy(() -> probe(process, Duration.ofSeconds(1), 1024).inspect(ASSET))
        .isInstanceOf(ContentImportException.class)
        .hasMessageContaining("could not be parsed");
  }

  @Test
  void failedProbeProcessIsRejectedWithoutExposingStderr() throws Exception {
    Process process = completedProcess("", "private path and media details", 1);

    assertThatThrownBy(() -> probe(process, Duration.ofSeconds(1), 1024).inspect(ASSET))
        .isInstanceOf(ContentImportException.class)
        .hasMessage("ffprobe rejected media")
        .hasMessageNotContaining("private path");
  }

  @Test
  void probeTimeoutForcefullyTerminatesProcess() throws Exception {
    Process process = mock(Process.class);
    when(process.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
    when(process.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
    when(process.isAlive()).thenReturn(true);
    when(process.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(false);
    when(process.destroyForcibly()).thenReturn(process);

    assertThatThrownBy(() -> probe(process, Duration.ofMillis(1), 1024).inspect(ASSET))
        .isInstanceOf(ContentImportException.class)
        .hasMessage("ffprobe timed out");
    verify(process).destroyForcibly();
  }

  @Test
  void stdoutBeyondConfiguredLimitIsRejectedWithoutUnboundedRead() throws Exception {
    Process process = completedProcess("x".repeat(65), "", 0);

    assertThatThrownBy(() -> probe(process, Duration.ofSeconds(1), 64).inspect(ASSET))
        .isInstanceOf(ContentImportException.class)
        .hasMessage("ffprobe stdout exceeded limit");
  }

  @Test
  void stderrBeyondConfiguredLimitIsRejectedWithoutUnboundedRead() throws Exception {
    int outputLimit = validOutput().getBytes(StandardCharsets.UTF_8).length + 10;
    Process process = completedProcess(validOutput(), "x".repeat(outputLimit + 1), 0);

    assertThatThrownBy(() -> probe(process, Duration.ofSeconds(1), outputLimit).inspect(ASSET))
        .isInstanceOf(ContentImportException.class)
        .hasMessage("ffprobe stderr exceeded limit");
  }

  private static FfprobeMediaProbe probe(Process process, Duration timeout, int outputLimit)
      throws Exception {
    FfprobeProcessLauncher launcher = mock(FfprobeProcessLauncher.class);
    when(launcher.start(anyString(), any(Path.class))).thenReturn(process);
    return probe(launcher, timeout, outputLimit);
  }

  private static FfprobeMediaProbe probe(
      FfprobeProcessLauncher launcher, Duration timeout, int outputLimit) {
    return new FfprobeMediaProbe(
        new ContentImporterProperties(false, null, null, "ffprobe", timeout, outputLimit),
        new ObjectMapper(),
        launcher);
  }

  private static Process completedProcess(String stdout, String stderr, int exitCode) {
    Process process = mock(Process.class);
    when(process.getInputStream())
        .thenReturn(new ByteArrayInputStream(stdout.getBytes(StandardCharsets.UTF_8)));
    when(process.getErrorStream())
        .thenReturn(new ByteArrayInputStream(stderr.getBytes(StandardCharsets.UTF_8)));
    when(process.isAlive()).thenReturn(false);
    when(process.exitValue()).thenReturn(exitCode);
    return process;
  }

  private static String validOutput() {
    return """
        {
          "format": {
            "format_name": "mov,mp4,m4a,3gp,3g2,mj2",
            "duration": "60.000"
          },
          "streams": [
            {
              "codec_type": "video",
              "codec_name": "h264",
              "width": 1080,
              "height": 1920,
              "avg_frame_rate": "30/1"
            },
            {
              "codec_type": "audio",
              "codec_name": "aac"
            }
          ]
        }
        """;
  }
}
