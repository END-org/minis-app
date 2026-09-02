package com.end.minisbackend.shared.error;

import com.end.minisbackend.shared.security.CorrelationId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

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
    logExpectedFailure(request, code, exception);
    return ResponseEntity.status(code.status()).body(errorWriter.responseFor(request, code));
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    BindException.class,
    ConstraintViolationException.class,
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestPartException.class,
    HttpMessageNotReadableException.class
  })
  ResponseEntity<ApiErrorResponse> handleValidation(HttpServletRequest request) {
    ApiErrorCode code = ApiErrorCode.VALIDATION_ERROR;
    logExpectedFailure(request, code, null);
    return ResponseEntity.status(code.status()).body(errorWriter.responseFor(request, code));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<ApiErrorResponse> handleMaxUploadSizeExceeded(HttpServletRequest request) {
    ApiErrorCode code = ApiErrorCode.UPLOAD_TOO_LARGE;
    logExpectedFailure(request, code, null);
    return ResponseEntity.status(code.status()).body(errorWriter.responseFor(request, code));
  }

  @ExceptionHandler(RuntimeException.class)
  ResponseEntity<ApiErrorResponse> handleRuntimeException(
      RuntimeException exception, HttpServletRequest request) {
    ApiErrorCode code = ApiErrorCode.INTERNAL_ERROR;
    LOGGER.error(
        "event=api_request_failed correlationId={} method={} path={} status={} "
            + "apiErrorCode={} exceptionCategory={}",
        CorrelationId.fromRequest(request),
        request.getMethod(),
        request.getRequestURI(),
        code.status().value(),
        code.name(),
        exception.getClass().getSimpleName(),
        exception);
    return ResponseEntity.status(code.status()).body(errorWriter.responseFor(request, code));
  }

  private static void logExpectedFailure(
      HttpServletRequest request, ApiErrorCode code, Exception exception) {
    String exceptionCategory =
        exception == null ? "validation" : exception.getClass().getSimpleName();
    String message =
        "event=api_request_failed correlationId={} method={} path={} status={} "
            + "apiErrorCode={} exceptionCategory={}";
    Object[] fields = {
      CorrelationId.fromRequest(request),
      request.getMethod(),
      request.getRequestURI(),
      code.status().value(),
      code.name(),
      exceptionCategory
    };
    if (code == ApiErrorCode.RESOURCE_CONFLICT
        || code == ApiErrorCode.ACCESS_DENIED
        || code.status().value() == 403) {
      LOGGER.warn(message, fields);
    } else {
      LOGGER.info(message, fields);
    }
  }
}
