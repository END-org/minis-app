package com.end.minisbackend.shared.security;

import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiErrorWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/** Writes authentication failures using the shared JSON error contract. */
@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {
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
}
