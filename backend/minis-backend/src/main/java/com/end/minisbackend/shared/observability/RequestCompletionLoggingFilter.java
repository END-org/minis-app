package com.end.minisbackend.shared.observability;

import com.end.minisbackend.shared.error.ApiErrorWriter;
import com.end.minisbackend.shared.security.AuthenticatedRequestLogContext;
import com.end.minisbackend.shared.security.CorrelationId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

/** Emits one safe completion log for each backend HTTP request. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestCompletionLoggingFilter extends OncePerRequestFilter {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(RequestCompletionLoggingFilter.class);
  private static final String EMPTY_FIELD = "-";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    long startedAt = System.nanoTime();
    try {
      filterChain.doFilter(request, response);
    } finally {
      logCompletedRequest(request, response, elapsedMillis(startedAt));
    }
  }

  private void logCompletedRequest(
      HttpServletRequest request, HttpServletResponse response, long durationMs) {
    int status = response.getStatus();
    String message =
        "event=http_request_completed correlationId={} method={} route={} path={} status={} "
            + "durationMs={} authenticated={} provisioned={} localUserId={} role={} apiErrorCode={}";
    Object[] fields = {
      CorrelationId.fromRequest(request),
      request.getMethod(),
      matchedRoute(request),
      sanitizedPath(request),
      status,
      durationMs,
      valueOrEmpty(request.getAttribute(AuthenticatedRequestLogContext.AUTHENTICATED_ATTRIBUTE)),
      valueOrEmpty(request.getAttribute(AuthenticatedRequestLogContext.PROVISIONED_ATTRIBUTE)),
      valueOrEmpty(request.getAttribute(AuthenticatedRequestLogContext.LOCAL_USER_ID_ATTRIBUTE)),
      valueOrEmpty(request.getAttribute(AuthenticatedRequestLogContext.ROLE_ATTRIBUTE)),
      valueOrEmpty(request.getAttribute(ApiErrorWriter.ERROR_CODE_ATTRIBUTE))
    };
    if (isNoisyInfrastructurePath(request)) {
      LOGGER.debug(message, fields);
    } else if (status >= 500) {
      LOGGER.error(message, fields);
    } else if (status == HttpServletResponse.SC_UNAUTHORIZED
        || status == HttpServletResponse.SC_FORBIDDEN) {
      LOGGER.warn(message, fields);
    } else {
      LOGGER.info(message, fields);
    }
  }

  private static long elapsedMillis(long startedAt) {
    return Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
  }

  private static String matchedRoute(HttpServletRequest request) {
    Object route = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
    return route == null ? EMPTY_FIELD : route.toString();
  }

  private static String sanitizedPath(HttpServletRequest request) {
    return Objects.toString(request.getRequestURI(), EMPTY_FIELD);
  }

  private static String valueOrEmpty(Object value) {
    return value == null ? EMPTY_FIELD : value.toString();
  }

  private static boolean isNoisyInfrastructurePath(HttpServletRequest request) {
    String path = sanitizedPath(request);
    return path.startsWith("/actuator/")
        || "/actuator".equals(path)
        || path.startsWith("/swagger-ui")
        || path.startsWith("/v3/api-docs");
  }
}
