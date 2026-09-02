package com.end.minisbackend.content.application.model.upload;

import com.end.minisbackend.content.domain.enums.ContentStatus;
import java.time.Instant;
import java.util.UUID;

public record ContentUploadView(
    UUID id,
    String title,
    String description,
    String category,
    String categoryLabel,
    ContentStatus status,
    String failureCode,
    String failureReason,
    Instant createdAt,
    Instant uploadStartedAt,
    Instant uploadCompletedAt,
    Instant publishedAt,
    String mediaContentType,
    Long mediaSizeBytes,
    Long durationMs) {}
