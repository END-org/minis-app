package com.end.minisbackend.content.application;

import java.time.Instant;
import java.util.UUID;

public record ContentUploadItem(
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
    String mediaContentType,
    Long mediaSizeBytes,
    Long durationMs,
    PublicContentItem content) {}
