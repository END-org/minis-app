package com.end.minisbackend.content.application;

import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import org.springframework.stereotype.Component;

@Component
public class EventMediaProfilePolicy {
  public static final String POLICY_ID = "event-mp4-h264-aac-vertical-v1";
  public static final String CONTENT_TYPE = "video/mp4";
  public static final long MAX_SIZE_BYTES = 200L * 1024L * 1024L;
  public static final long MAX_DURATION_MS = 90_000L;
  public static final int MIN_WIDTH = 720;
  public static final int MAX_WIDTH = 1080;
  public static final int MIN_HEIGHT = 1280;
  public static final int MAX_HEIGHT = 1920;
  public static final double MAX_FRAME_RATE = 30.0d;

  public MediaValidationResult validate(MediaInspection inspection, long sizeBytes) {
    validateSize(sizeBytes);
    if (inspection == null) {
      throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
    }
    if (inspection.durationMs() <= 0 || inspection.durationMs() > MAX_DURATION_MS) {
      throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
    }
    if (!hasContainer(inspection.container(), "mp4") || !"h264".equals(inspection.videoCodec())) {
      throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
    }
    if (inspection.audioCodec() != null && !"aac".equals(inspection.audioCodec())) {
      throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
    }
    if (inspection.width() < MIN_WIDTH
        || inspection.width() > MAX_WIDTH
        || inspection.height() < MIN_HEIGHT
        || inspection.height() > MAX_HEIGHT
        || inspection.height() <= inspection.width()) {
      throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
    }
    if (inspection.frameRate() != null
        && (!Double.isFinite(inspection.frameRate())
            || inspection.frameRate() <= 0d
            || inspection.frameRate() > MAX_FRAME_RATE)) {
      throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
    }
    int expectedStreams = inspection.audioCodec() == null ? 1 : 2;
    if (inspection.streamCount() != expectedStreams) {
      throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
    }
    return new MediaValidationResult(CONTENT_TYPE, sizeBytes, inspection.durationMs());
  }

  public void validateSize(long sizeBytes) {
    if (sizeBytes <= 0 || sizeBytes > MAX_SIZE_BYTES) {
      throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
    }
  }

  private static boolean hasContainer(String detectedContainers, String requiredContainer) {
    if (detectedContainers == null) {
      return false;
    }
    return java.util.Arrays.stream(detectedContainers.split(","))
        .map(String::trim)
        .anyMatch(requiredContainer::equals);
  }
}
