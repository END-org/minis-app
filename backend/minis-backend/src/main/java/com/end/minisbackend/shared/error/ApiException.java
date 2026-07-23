package com.end.minisbackend.shared.error;

/** Application exception carrying a stable client-facing error code. */
public class ApiException extends RuntimeException {
  private final ApiErrorCode errorCode;

  public ApiException(ApiErrorCode errorCode) {
    super(errorCode.defaultMessage());
    this.errorCode = errorCode;
  }

  public ApiException(ApiErrorCode errorCode, Throwable cause) {
    super(errorCode.defaultMessage(), cause);
    this.errorCode = errorCode;
  }

  public ApiErrorCode errorCode() {
    return errorCode;
  }
}
