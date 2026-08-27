package com.end.minisbackend.content.api;

import java.time.Instant;
import java.util.UUID;

public record ContentResponse(
    UUID id,
    ContentCreatorResponse creator,
    String title,
    String description,
    String category,
    String categoryLabel,
    ContentMediaResponse media,
    Instant publishedAt) {}
