package com.end.minisbackend.shared.security;

import com.end.minisbackend.shared.error.ApiErrorCode;
import org.springframework.security.core.AuthenticationException;

/** Authentication failure with a stable API error code. */
public class MinisAuthenticationException extends AuthenticationException {
  private final ApiErrorCode errorCode;

  public MinisAuthenticationException(ApiErrorCode errorCode) {
    super(errorCode.defaultMessage());
    this.errorCode = errorCode;
  }

  public MinisAuthenticationException(ApiErrorCode errorCode, Throwable cause) {
    super(errorCode.defaultMessage(), cause);
    this.errorCode = errorCode;
  }

  public ApiErrorCode errorCode() {
    return errorCode;
  }
}
