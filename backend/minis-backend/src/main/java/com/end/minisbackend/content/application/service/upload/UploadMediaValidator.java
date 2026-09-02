package com.end.minisbackend.content.application.service.upload;

import com.end.minisbackend.content.application.port.ContentUploadSettings;
import com.end.minisbackend.content.application.port.MediaProbe;
import com.end.minisbackend.content.application.port.UploadVideoSource;
import com.end.minisbackend.content.domain.enums.ContentUploadFailureCode;
import com.end.minisbackend.content.domain.model.media.MediaInspection;
import com.end.minisbackend.content.domain.model.media.MediaValidationResult;
import com.end.minisbackend.content.domain.policy.EventMediaProfilePolicy;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class UploadMediaValidator {
  private static final int ORIGINAL_FILENAME_MAX = 255;
  private static final byte[] MP4_FILE_TYPE_BOX = {'f', 't', 'y', 'p'};

  private final MediaProbe mediaProbe;
  private final EventMediaProfilePolicy mediaProfilePolicy;
  private final ContentUploadSettings uploadSettings;

  public UploadMediaValidator(
      MediaProbe mediaProbe,
      EventMediaProfilePolicy mediaProfilePolicy,
      ContentUploadSettings uploadSettings) {
    this.mediaProbe = mediaProbe;
    this.mediaProfilePolicy = mediaProfilePolicy;
    this.uploadSettings = uploadSettings;
  }

  Path stage(UploadVideoSource video) throws IOException {
    if (video == null) {
      throw rejected(ContentUploadFailureCode.EMPTY_VIDEO, ApiErrorCode.VALIDATION_ERROR);
    }
    validateFileSize(video.size());
    validateOriginalFilename(video.originalFilename());
    validateContentType(video.contentType());
    Files.createDirectories(uploadSettings.tempDirectory());
    Path tempFile = Files.createTempFile(uploadSettings.tempDirectory(), "upload-", ".mp4");
    video.transferTo(tempFile);
    validateFileSize(Files.size(tempFile));
    validateMp4Signature(tempFile);
    return tempFile;
  }

  MediaValidationResult inspect(Path tempFile) throws IOException {
    return validateMediaProfile(mediaProbe.inspect(tempFile), Files.size(tempFile));
  }

  private void validateFileSize(long sizeBytes) {
    if (sizeBytes <= 0) {
      throw rejected(ContentUploadFailureCode.EMPTY_VIDEO, ApiErrorCode.UPLOAD_VALIDATION_FAILED);
    }
    if (sizeBytes > uploadSettings.maxFileSizeBytes()) {
      throw rejected(ContentUploadFailureCode.UPLOAD_TOO_LARGE, ApiErrorCode.UPLOAD_TOO_LARGE);
    }
  }

  private static void validateOriginalFilename(String originalFilename) {
    if (!StringUtils.hasText(originalFilename)) {
      throw rejected(ContentUploadFailureCode.INVALID_FILENAME, ApiErrorCode.VALIDATION_ERROR);
    }
    String trimmed = originalFilename.trim();
    boolean unsafe =
        trimmed.length() > ORIGINAL_FILENAME_MAX
            || trimmed.contains("/")
            || trimmed.contains("\\")
            || trimmed.chars().anyMatch(Character::isISOControl)
            || !trimmed.toLowerCase(Locale.ROOT).endsWith(".mp4");
    if (unsafe) {
      throw rejected(ContentUploadFailureCode.INVALID_FILENAME, ApiErrorCode.VALIDATION_ERROR);
    }
  }

  private static void validateContentType(String contentType) {
    String normalized =
        contentType == null ? "" : contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    if (!EventMediaProfilePolicy.CONTENT_TYPE.equals(normalized)) {
      throw rejected(
          ContentUploadFailureCode.UNSUPPORTED_CONTENT_TYPE, ApiErrorCode.UPLOAD_VALIDATION_FAILED);
    }
  }

  private static void validateMp4Signature(Path tempFile) throws IOException {
    byte[] header = new byte[12];
    int bytesRead;
    try (InputStream inputStream = Files.newInputStream(tempFile)) {
      bytesRead = inputStream.read(header);
    }
    if (bytesRead < header.length
        || header[4] != MP4_FILE_TYPE_BOX[0]
        || header[5] != MP4_FILE_TYPE_BOX[1]
        || header[6] != MP4_FILE_TYPE_BOX[2]
        || header[7] != MP4_FILE_TYPE_BOX[3]) {
      throw rejected(
          ContentUploadFailureCode.UNSUPPORTED_MEDIA_PROFILE,
          ApiErrorCode.UPLOAD_VALIDATION_FAILED);
    }
  }

  private MediaValidationResult validateMediaProfile(MediaInspection inspection, long sizeBytes) {
    try {
      return mediaProfilePolicy.validate(inspection, sizeBytes);
    } catch (ApiException exception) {
      throw new ContentUploadRejectedException(
          ContentUploadFailureCode.UNSUPPORTED_MEDIA_PROFILE,
          ApiErrorCode.UPLOAD_VALIDATION_FAILED,
          exception);
    }
  }

  private static ContentUploadRejectedException rejected(
      ContentUploadFailureCode failureCode, ApiErrorCode apiErrorCode) {
    return new ContentUploadRejectedException(failureCode, apiErrorCode);
  }
}
