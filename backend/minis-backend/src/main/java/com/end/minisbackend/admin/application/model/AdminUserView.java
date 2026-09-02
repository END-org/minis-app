package com.end.minisbackend.admin.application.model;

import com.end.minisbackend.creator.application.model.CreatorProfileView;
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
