package com.end.minisbackend.shared.error;

import com.end.minisbackend.shared.security.CorrelationId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Writes the canonical MINIS error response without exposing internal details. */
@Component
public class ApiErrorWriter {
  public static final String ERROR_CODE_ATTRIBUTE = ApiErrorWriter.class.getName() + ".errorCode";
  public static final String ERROR_STATUS_ATTRIBUTE =
      ApiErrorWriter.class.getName() + ".errorStatus";

  private final ObjectMapper objectMapper;
  private final Clock clock;

  public ApiErrorWriter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.clock = Clock.systemUTC();
  }

  public ApiErrorResponse responseFor(HttpServletRequest request, ApiErrorCode code) {
    request.setAttribute(ERROR_CODE_ATTRIBUTE, code.name());
    request.setAttribute(ERROR_STATUS_ATTRIBUTE, code.status().value());
    return new ApiErrorResponse(
        Instant.now(clock),
        code.status().value(),
        code.name(),
        code.defaultMessage(),
        request.getRequestURI(),
        CorrelationId.fromRequest(request));
  }

  public void write(HttpServletRequest request, HttpServletResponse response, ApiErrorCode code)
      throws IOException {
    response.setStatus(code.status().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    response.setHeader(CorrelationId.HEADER_NAME, CorrelationId.fromRequest(request));
    objectMapper.writeValue(response.getWriter(), responseFor(request, code));
  }
}
