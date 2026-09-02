package com.end.minisbackend.admin.api.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminCreatorProfileResponse(
    UUID userId,
    String slug,
    String displayName,
    String bio,
    String avatarUrl,
    boolean verified,
    Instant createdAt,
    Instant updatedAt) {}
