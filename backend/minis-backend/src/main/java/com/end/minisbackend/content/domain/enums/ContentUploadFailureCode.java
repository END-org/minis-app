package com.end.minisbackend.content.domain.enums;

/** Safe, bounded upload failure categories stored for creator-facing My Uploads. */
public enum ContentUploadFailureCode {
  EMPTY_VIDEO("The selected video is empty."),
  UPLOAD_TOO_LARGE("The selected video is larger than the MINIS event limit."),
  INVALID_FILENAME("Use a single MP4 file name for this event upload."),
  UNSUPPORTED_CONTENT_TYPE("The browser did not identify this as an MP4 video."),
  UNSUPPORTED_MEDIA_PROFILE("The video does not match the MINIS event media rules."),
  TEMPORARY_FILE_FAILED("The upload could not be prepared safely."),
  STORAGE_UPLOAD_FAILED("The video could not be stored. Please start a new upload attempt."),
  PUBLICATION_FAILED("The video could not be published. Please start a new upload attempt."),
  STALE_UPLOAD("The upload did not finish and was marked failed by reconciliation.");

  private final String defaultReason;

  ContentUploadFailureCode(String defaultReason) {
    this.defaultReason = defaultReason;
  }

  public String defaultReason() {
    return defaultReason;
  }
}
