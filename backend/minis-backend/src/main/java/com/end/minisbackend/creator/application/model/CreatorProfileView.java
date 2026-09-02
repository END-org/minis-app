package com.end.minisbackend.creator.application.model;

import java.time.Instant;
import java.util.UUID;

public record CreatorProfileView(
    UUID userId,
    String slug,
    String displayName,
    String bio,
    String avatarUrl,
    boolean verified,
    Instant createdAt,
    Instant updatedAt) {}
