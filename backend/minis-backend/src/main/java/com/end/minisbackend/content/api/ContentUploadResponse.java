package com.end.minisbackend.content.api;

import java.time.Instant;
import java.util.UUID;

public record ContentUploadResponse(
    UUID id,
    String title,
    String description,
    String category,
    String categoryLabel,
    String status,
    String failureCode,
    String failureReason,
    Instant createdAt,
    Instant uploadStartedAt,
    Instant uploadCompletedAt,
    Instant publishedAt,
    ContentUploadMediaResponse media,
    ContentResponse content) {}
