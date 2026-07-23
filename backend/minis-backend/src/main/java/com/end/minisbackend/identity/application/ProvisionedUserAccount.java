package com.end.minisbackend.identity.application;

import java.time.Instant;
import java.util.UUID;

/** Safe account projection returned by authentication-related API endpoints. */
public record ProvisionedUserAccount(
    UUID id,
    String email,
    boolean emailVerified,
    String displayName,
    String avatarUrl,
    String bio,
    String role,
    String status,
    Instant createdAt) {}
