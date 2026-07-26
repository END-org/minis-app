package com.end.minisbackend.identity.api;

import java.time.Instant;
import java.util.UUID;

/** Account projection returned after Firebase identity bootstrap. */
public record AuthBootstrapResponse(
    UUID id,
    String email,
    boolean emailVerified,
    String displayName,
    String avatarUrl,
    String bio,
    String role,
    String status,
    Instant createdAt) {}
