package com.end.minisbackend.content.api.dto.feed;

import java.util.UUID;

public record ContentCreatorResponse(
    UUID userId, String slug, String displayName, String avatarUrl, boolean verified) {}
