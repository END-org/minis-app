package com.end.minisbackend.creator.application;

public record GrantCreatorProfileCommand(
    String slug, String displayName, String bio, String avatarUrl) {}
