package com.end.minisbackend.creator.api;

import java.time.Instant;
import java.util.UUID;

public record CreatorResponse(
    UUID userId,
    String slug,
    String displayName,
    String bio,
    String avatarUrl,
    boolean verified,
    boolean following,
    Instant createdAt,
    Instant updatedAt) {}
