package com.end.minisbackend.content.application.model.feed;

import com.end.minisbackend.content.domain.enums.ContentCategory;
import java.time.Instant;
import java.util.UUID;

public record PublishedContentView(
    UUID id,
    UUID creatorUserId,
    String creatorSlug,
    String creatorDisplayName,
    String creatorAvatarUrl,
    boolean creatorVerified,
    String title,
    String description,
    ContentCategory category,
    String mediaObjectKey,
    String mediaContentType,
    long mediaSizeBytes,
    Long durationMs,
    Instant publishedAt) {}
