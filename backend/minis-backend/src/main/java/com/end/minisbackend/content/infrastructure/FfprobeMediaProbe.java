package com.end.minisbackend.content.infrastructure;

import com.end.minisbackend.content.application.ContentImportException;
import com.end.minisbackend.content.application.MediaInspection;
import com.end.minisbackend.content.application.MediaProbe;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
class FfprobeMediaProbe implements MediaProbe {
  private static final long OUTPUT_COLLECTION_TIMEOUT_SECONDS = 1L;
  private static final long PROCESS_POLL_MILLIS = 25L;

  private final ContentImporterProperties properties;
  private final ObjectMapper objectMapper;
  private final FfprobeProcessLauncher processLauncher;

  FfprobeMediaProbe(
      ContentImporterProperties properties,
      ObjectMapper objectMapper,
      FfprobeProcessLauncher processLauncher) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.processLauncher = processLauncher;
  }

  @Override
  public MediaInspection inspect(Path file) {
    properties.requireProbeConfigured();
    try {
      Process startedProcess = processLauncher.start(properties.ffprobePath(), file);
      try (ExecutorService outputReaders = Executors.newVirtualThreadPerTaskExecutor()) {
        Future<byte[]> stdout =
            outputReaders.submit(
                () ->
                    readBounded(
                        startedProcess.getInputStream(),
                        properties.maxProbeOutputBytes(),
                        "stdout"));
        Future<byte[]> stderr =
            outputReaders.submit(
                () ->
                    readBounded(
                        startedProcess.getErrorStream(),
                        properties.maxProbeOutputBytes(),
                        "stderr"));
        try {
          waitForProcess(startedProcess, stdout, stderr);
          String json = new String(output(stdout), StandardCharsets.UTF_8);
          output(stderr);
          if (startedProcess.exitValue() != 0) {
            throw new ContentImportException("ffprobe rejected media");
          }
          return parseInspection(json);
        } finally {
          terminate(startedProcess);
        }
      }
    } catch (IOException exception) {
      throw new ContentImportException(
          "ffprobe is not available or could not inspect media", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new ContentImportException("ffprobe inspection was interrupted", exception);
    }
  }

  private void waitForProcess(Process process, Future<byte[]> stdout, Future<byte[]> stderr)
      throws InterruptedException {
    long deadline = System.nanoTime() + properties.probeTimeout().toNanos();
    while (process.isAlive()) {
      failIfReaderFailed(stdout);
      failIfReaderFailed(stderr);
      long remaining = deadline - System.nanoTime();
      if (remaining <= 0L) {
        throw new ContentImportException("ffprobe timed out");
      }
      process.waitFor(
          Math.min(remaining, TimeUnit.MILLISECONDS.toNanos(PROCESS_POLL_MILLIS)),
          TimeUnit.NANOSECONDS);
    }
    failIfReaderFailed(stdout);
    failIfReaderFailed(stderr);
  }

  private static void failIfReaderFailed(Future<byte[]> output) {
    if (output.isDone()) {
      output(output);
    }
  }

  private static byte[] output(Future<byte[]> output) {
    try {
      return output.get(OUTPUT_COLLECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    } catch (ExecutionException exception) {
      if (exception.getCause() instanceof ContentImportException importException) {
        throw importException;
      }
      throw new ContentImportException("ffprobe output could not be read", exception.getCause());
    } catch (TimeoutException exception) {
      throw new ContentImportException("ffprobe output could not be collected", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new ContentImportException("ffprobe output collection was interrupted", exception);
    }
  }

  private static byte[] readBounded(InputStream input, int maxBytes, String streamName) {
    try (input) {
      byte[] bytes = input.readNBytes(maxBytes + 1);
      if (bytes.length > maxBytes) {
        throw new ContentImportException("ffprobe " + streamName + " exceeded limit");
      }
      return bytes;
    } catch (IOException exception) {
      throw new ContentImportException("ffprobe " + streamName + " could not be read", exception);
    }
  }

  private static void terminate(Process process) {
    if (process != null && process.isAlive()) {
      process.destroyForcibly();
      try {
        process.waitFor(1L, TimeUnit.SECONDS);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private MediaInspection parseInspection(String json) {
    try {
      JsonNode root = objectMapper.readTree(json);
      if (root == null || !root.isObject()) {
        throw new ContentImportException("ffprobe output could not be parsed");
      }
      JsonNode format = root.path("format");
      JsonNode video = firstStream(root, "video");
      if (video == null) {
        throw new ContentImportException("ffprobe video stream is missing");
      }
      JsonNode audio = firstStream(root, "audio");
      return new MediaInspection(
          format.path("format_name").asString(),
          video.path("codec_name").asString(),
          audio == null ? null : audio.path("codec_name").asString(),
          durationMs(format),
          video.path("width").asInt(),
          video.path("height").asInt(),
          frameRate(video),
          root.path("streams").size());
    } catch (tools.jackson.core.JacksonException | IllegalArgumentException exception) {
      throw new ContentImportException("ffprobe output could not be parsed", exception);
    }
  }

  private static JsonNode firstStream(JsonNode root, String type) {
    for (JsonNode stream : root.path("streams")) {
      if (type.equals(stream.path("codec_type").asString())) {
        return stream;
      }
    }
    return null;
  }

  private static long durationMs(JsonNode format) {
    String duration = format.path("duration").asString();
    if (duration == null || duration.isBlank()) {
      throw new ContentImportException("ffprobe duration is missing");
    }
    return Math.round(Double.parseDouble(duration) * 1000d);
  }

  private static Double frameRate(JsonNode video) {
    return Optional.ofNullable(video.path("avg_frame_rate").asString())
        .filter(value -> !value.isBlank() && !"0/0".equals(value))
        .map(FfprobeMediaProbe::parseRate)
        .orElse(null);
  }

  private static double parseRate(String value) {
    String[] parts = value.split("/", -1);
    if (parts.length != 2) {
      return Double.parseDouble(value);
    }
    double numerator = Double.parseDouble(parts[0]);
    double denominator = Double.parseDouble(parts[1]);
    if (denominator == 0d) {
      return 0d;
    }
    return numerator / denominator;
  }
}
