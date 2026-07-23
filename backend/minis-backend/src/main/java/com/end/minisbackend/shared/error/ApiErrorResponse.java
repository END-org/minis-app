package com.end.minisbackend.shared.error;

import java.time.Instant;

/** JSON error response shared by MVC and Spring Security failures. */
public record ApiErrorResponse(
    Instant timestamp,
    int status,
    String code,
    String message,
    String path,
    String correlationId) {}
