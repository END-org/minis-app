package com.end.minisbackend.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.DefaultCorsProcessor;

/** Logs safe CORS rejection details at Spring's CORS processing boundary. */
public class LoggingCorsProcessor extends DefaultCorsProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingCorsProcessor.class);
  private static final String EMPTY_FIELD = "-";

  @Override
  public boolean processRequest(
      CorsConfiguration config, HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    boolean valid = super.processRequest(config, request, response);
    if (!valid) {
      logRejectedRequest(config, request);
    }
    return valid;
  }

  private void logRejectedRequest(CorsConfiguration config, HttpServletRequest request) {
    LOGGER.warn(
        "event=cors_request_rejected correlationId={} method={} path={} origin={} "
            + "requestedMethod={} requestedHeaders={} allowedOrigins={} rejectionCategory={}",
        CorrelationId.fromRequest(request),
        request.getMethod(),
        request.getRequestURI(),
        valueOrEmpty(request.getHeader(HttpHeaders.ORIGIN)),
        valueOrEmpty(request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD)),
        requestedHeaderNames(request),
        allowedOrigins(config),
        rejectionCategory(config, request));
  }

  private static String rejectionCategory(CorsConfiguration config, HttpServletRequest request) {
    if (config == null || allowedOrigins(config).isEmpty()) {
      return "missing_configuration";
    }
    String origin = request.getHeader(HttpHeaders.ORIGIN);
    try {
      if (!CorsUtils.isCorsRequest(request)) {
        return "other";
      }
    } catch (IllegalArgumentException exception) {
      return "origin_not_allowed";
    }
    if (config.checkOrigin(origin) == null) {
      return "origin_not_allowed";
    }
    if (CorsUtils.isPreFlightRequest(request)) {
      HttpMethod requestedMethod = requestedMethod(request);
      if (requestedMethod == null || config.checkHttpMethod(requestedMethod) == null) {
        return "method_not_allowed";
      }
      List<String> requestedHeaders = requestedHeaderNames(request);
      if (!requestedHeaders.isEmpty() && config.checkHeaders(requestedHeaders) == null) {
        return "headers_not_allowed";
      }
    }
    return "other";
  }

  private static HttpMethod requestedMethod(HttpServletRequest request) {
    String method = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
    if (!StringUtils.hasText(method)) {
      return null;
    }
    try {
      return HttpMethod.valueOf(method.trim());
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  private static List<String> requestedHeaderNames(HttpServletRequest request) {
    String value = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
    if (!StringUtils.hasText(value)) {
      return List.of();
    }
    return Arrays.stream(value.split(",")).map(String::trim).filter(StringUtils::hasText).toList();
  }

  private static List<String> allowedOrigins(CorsConfiguration config) {
    if (config == null) {
      return List.of();
    }
    List<String> origins = config.getAllowedOrigins();
    return origins == null ? List.of() : origins;
  }

  private static String valueOrEmpty(String value) {
    return StringUtils.hasText(value) ? value.trim() : EMPTY_FIELD;
  }
}
