package com.end.minisbackend.admin.api;

import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
    UUID id,
    String email,
    String displayName,
    String avatarUrl,
    String bio,
    String role,
    String status,
    Instant createdAt,
    Instant updatedAt,
    AdminCreatorProfileResponse creatorProfile) {}
