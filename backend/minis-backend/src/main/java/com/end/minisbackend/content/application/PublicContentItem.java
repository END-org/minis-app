package com.end.minisbackend.content.application;

import java.time.Instant;
import java.util.UUID;

public record PublicContentItem(
    UUID id,
    CreatorAttribution creator,
    String title,
    String description,
    String category,
    String categoryLabel,
    SignedMediaReference media,
    Instant publishedAt,
    long likeCount,
    boolean liked,
    boolean saved,
    boolean following) {}
