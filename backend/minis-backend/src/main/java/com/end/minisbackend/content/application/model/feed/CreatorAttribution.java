package com.end.minisbackend.content.application.model.feed;

import java.util.UUID;

public record CreatorAttribution(
    UUID userId, String slug, String displayName, String avatarUrl, boolean verified) {}
