package com.end.minisbackend.creator.application.model;

public record GrantCreatorProfileCommand(
    String slug, String displayName, String bio, String avatarUrl) {}
