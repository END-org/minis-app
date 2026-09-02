package com.end.minisbackend.content.application;

import com.end.minisbackend.content.domain.ContentUploadFailureCode;
import com.end.minisbackend.shared.error.ApiErrorCode;

final class ContentUploadRejectedException extends RuntimeException {
  private final ContentUploadFailureCode failureCode;
  private final ApiErrorCode apiErrorCode;

  ContentUploadRejectedException(ContentUploadFailureCode failureCode, ApiErrorCode apiErrorCode) {
    super(failureCode.defaultReason());
    this.failureCode = failureCode;
    this.apiErrorCode = apiErrorCode;
  }

  ContentUploadRejectedException(
      ContentUploadFailureCode failureCode, ApiErrorCode apiErrorCode, Throwable cause) {
    super(failureCode.defaultReason(), cause);
    this.failureCode = failureCode;
    this.apiErrorCode = apiErrorCode;
  }

  ContentUploadFailureCode failureCode() {
    return failureCode;
  }

  ApiErrorCode apiErrorCode() {
    return apiErrorCode;
  }
}
