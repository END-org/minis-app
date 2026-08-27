package com.end.minisbackend.content.application;

import java.time.Instant;

public record SignedMediaReference(
    String url, Instant expiresAt, String contentType, long sizeBytes, Long durationMs) {}
