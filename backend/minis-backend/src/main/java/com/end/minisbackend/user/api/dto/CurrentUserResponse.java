package com.end.minisbackend.user.api.dto;

import java.time.Instant;
import java.util.UUID;

/** Current local MINIS account projection returned by /api/v1/me. */
public record CurrentUserResponse(
    UUID id,
    String email,
    boolean emailVerified,
    String displayName,
    String avatarUrl,
    String bio,
    String role,
    String status,
    Instant createdAt) {}
