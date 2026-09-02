package com.end.minisbackend.content.api.dto.feed;

import java.time.Instant;

public record ContentMediaResponse(
    String url, Instant expiresAt, String contentType, long sizeBytes, Long durationMs) {}
