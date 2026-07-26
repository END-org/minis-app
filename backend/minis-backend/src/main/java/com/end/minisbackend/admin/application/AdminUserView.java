package com.end.minisbackend.admin.application;

import com.end.minisbackend.creator.application.CreatorProfileView;
import java.time.Instant;
import java.util.UUID;

public record AdminUserView(
    UUID id,
    String email,
    String displayName,
    String avatarUrl,
    String bio,
    String role,
    String status,
    Instant createdAt,
    Instant updatedAt,
    CreatorProfileView creatorProfile) {}
