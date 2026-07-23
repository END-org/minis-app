package com.end.minisbackend.shared.security;

import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiErrorWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Writes authorization failures using the shared JSON error contract. */
@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {
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
    errorWriter.write(request, response, ApiErrorCode.ACCESS_DENIED);
  }
}
