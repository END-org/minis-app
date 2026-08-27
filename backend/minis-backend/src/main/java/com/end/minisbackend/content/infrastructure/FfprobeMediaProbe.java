package com.end.minisbackend.content.infrastructure;

import com.end.minisbackend.content.application.ContentImportException;
import com.end.minisbackend.content.application.MediaInspection;
import com.end.minisbackend.content.application.MediaProbe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
class FfprobeMediaProbe implements MediaProbe {
  private final ContentImporterProperties properties;
  private final ObjectMapper objectMapper;

  FfprobeMediaProbe(ContentImporterProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @Override
  public MediaInspection inspect(Path file) {
    Path output = probeOutputFile();
    try {
      Process process =
          new ProcessBuilder(
                  properties.ffprobePath(),
                  "-v",
                  "error",
                  "-print_format",
                  "json",
                  "-show_format",
                  "-show_streams",
                  file.toString())
              .redirectErrorStream(true)
              .redirectOutput(output.toFile())
              .start();
      boolean completed =
          process.waitFor(properties.probeTimeout().toMillis(), TimeUnit.MILLISECONDS);
      if (!completed) {
        process.destroyForcibly();
        throw new ContentImportException("ffprobe timed out");
      }
      String json = readBoundedOutput(output);
      if (process.exitValue() != 0) {
        throw new ContentImportException("ffprobe rejected media");
      }
      return parseInspection(json);
    } catch (IOException exception) {
      throw new ContentImportException(
          "ffprobe is not available or could not inspect media", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new ContentImportException("ffprobe inspection was interrupted", exception);
    } finally {
      deleteQuietly(output);
    }
  }

  private MediaInspection parseInspection(String json) {
    try {
      JsonNode root = objectMapper.readTree(json);
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

  private String readBoundedOutput(Path output) throws IOException {
    byte[] bytes = Files.readAllBytes(output);
    if (bytes.length > properties.maxProbeOutputBytes()) {
      throw new ContentImportException("ffprobe output exceeded limit");
    }
    return new String(bytes, StandardCharsets.UTF_8);
  }

  private static Path probeOutputFile() {
    try {
      return Files.createTempFile("minis-ffprobe-", ".json");
    } catch (IOException exception) {
      throw new ContentImportException("ffprobe output file could not be created", exception);
    }
  }

  private static void deleteQuietly(Path output) {
    try {
      Files.deleteIfExists(output);
    } catch (IOException ignored) {
      // Temporary probe output is not part of product state.
    }
  }
}
