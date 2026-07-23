package com.end.minisbackend.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Converts application and MVC exceptions into the shared API error format. */
@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private final ApiErrorWriter errorWriter;

  public GlobalExceptionHandler(ApiErrorWriter errorWriter) {
    this.errorWriter = errorWriter;
  }

  @ExceptionHandler(ApiException.class)
  ResponseEntity<ApiErrorResponse> handleApiException(
      ApiException exception, HttpServletRequest request) {
    ApiErrorCode code = exception.errorCode();
    return ResponseEntity.status(code.status()).body(errorWriter.responseFor(request, code));
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    BindException.class,
    ConstraintViolationException.class
  })
  ResponseEntity<ApiErrorResponse> handleValidation(HttpServletRequest request) {
    ApiErrorCode code = ApiErrorCode.VALIDATION_ERROR;
    return ResponseEntity.status(code.status()).body(errorWriter.responseFor(request, code));
  }

  @ExceptionHandler(RuntimeException.class)
  ResponseEntity<ApiErrorResponse> handleRuntimeException(
      RuntimeException exception, HttpServletRequest request) {
    LOGGER.error(
        "Unhandled runtime exception for request path {}", request.getRequestURI(), exception);
    ApiErrorCode code = ApiErrorCode.INTERNAL_ERROR;
    return ResponseEntity.status(code.status()).body(errorWriter.responseFor(request, code));
  }
}
