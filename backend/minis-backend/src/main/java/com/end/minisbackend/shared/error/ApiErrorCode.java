package com.end.minisbackend.shared.error;

import org.springframework.http.HttpStatus;

/** Stable API error codes returned to clients. */
public enum ApiErrorCode {
  MISSING_AUTH_TOKEN(HttpStatus.UNAUTHORIZED, "Authentication token is required."),
  INVALID_AUTH_TOKEN(HttpStatus.UNAUTHORIZED, "Authentication token is invalid."),
  EXPIRED_AUTH_TOKEN(HttpStatus.UNAUTHORIZED, "Authentication token has expired."),
  EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "Email address must be verified before bootstrap."),
  ACCOUNT_NOT_PROVISIONED(HttpStatus.FORBIDDEN, "MINIS account has not been provisioned."),
  ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "MINIS account is suspended."),
  ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "MINIS account is disabled."),
  ACCESS_DENIED(HttpStatus.FORBIDDEN, "Access is denied."),
  VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Request validation failed."),
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred.");

  private final HttpStatus status;
  private final String defaultMessage;

  ApiErrorCode(HttpStatus status, String defaultMessage) {
    this.status = status;
    this.defaultMessage = defaultMessage;
  }

  public HttpStatus status() {
    return status;
  }

  public String defaultMessage() {
    return defaultMessage;
  }
}
