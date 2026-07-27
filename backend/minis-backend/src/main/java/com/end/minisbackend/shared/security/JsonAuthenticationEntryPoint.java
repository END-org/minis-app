package com.end.minisbackend.shared.security;

import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiErrorWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/** Writes authentication failures using the shared JSON error contract. */
@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {
  private static final Logger LOGGER = LoggerFactory.getLogger(JsonAuthenticationEntryPoint.class);

  private final ApiErrorWriter errorWriter;

  public JsonAuthenticationEntryPoint(ApiErrorWriter errorWriter) {
    this.errorWriter = errorWriter;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authenticationException)
      throws IOException {
    ApiErrorCode code = resolveCode(request, authenticationException);
    LOGGER.warn(
        "event=security_authentication_failed correlationId={} method={} path={} "
            + "authenticated={} localUserId={} role={} apiErrorCode={} component={} "
            + "exceptionCategory={}",
        CorrelationId.fromRequest(request),
        request.getMethod(),
        request.getRequestURI(),
        valueOrEmpty(request.getAttribute(AuthenticatedRequestLogContext.AUTHENTICATED_ATTRIBUTE)),
        valueOrEmpty(request.getAttribute(AuthenticatedRequestLogContext.LOCAL_USER_ID_ATTRIBUTE)),
        valueOrEmpty(request.getAttribute(AuthenticatedRequestLogContext.ROLE_ATTRIBUTE)),
        code.name(),
        "JsonAuthenticationEntryPoint",
        authenticationException.getClass().getSimpleName());
    errorWriter.write(request, response, code);
  }

  private ApiErrorCode resolveCode(
      HttpServletRequest request, AuthenticationException authenticationException) {
    if (authenticationException instanceof MinisAuthenticationException minisException) {
      return minisException.errorCode();
    }
    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authorization == null || authorization.isBlank()) {
      return ApiErrorCode.MISSING_AUTH_TOKEN;
    }
    return ApiErrorCode.INVALID_AUTH_TOKEN;
  }

  private static String valueOrEmpty(Object value) {
    return value == null ? "-" : value.toString();
  }
}
