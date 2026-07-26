package com.end.minisbackend.shared.security;

import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiErrorWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Writes authorization failures using the shared JSON error contract. */
@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(JsonAccessDeniedHandler.class);

  private final ApiErrorWriter errorWriter;

  public JsonAccessDeniedHandler(ApiErrorWriter errorWriter) {
    this.errorWriter = errorWriter;
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    LOGGER.warn(
        "event=security_access_denied correlationId={} method={} path={} authenticated={} "
            + "localUserId={} role={} apiErrorCode={} component={} exceptionCategory={}",
        CorrelationId.fromRequest(request),
        request.getMethod(),
        request.getRequestURI(),
        valueOrEmpty(request.getAttribute(AuthenticatedRequestLogContext.AUTHENTICATED_ATTRIBUTE)),
        valueOrEmpty(request.getAttribute(AuthenticatedRequestLogContext.LOCAL_USER_ID_ATTRIBUTE)),
        valueOrEmpty(request.getAttribute(AuthenticatedRequestLogContext.ROLE_ATTRIBUTE)),
        ApiErrorCode.ACCESS_DENIED.name(),
        "JsonAccessDeniedHandler",
        accessDeniedException.getClass().getSimpleName());
    errorWriter.write(request, response, ApiErrorCode.ACCESS_DENIED);
  }

  private static String valueOrEmpty(Object value) {
    return value == null ? "-" : value.toString();
  }
}
