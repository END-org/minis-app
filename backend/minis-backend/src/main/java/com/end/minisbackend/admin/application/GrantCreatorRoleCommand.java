package com.end.minisbackend.admin.application;

public record GrantCreatorRoleCommand(
    String slug, String displayName, String bio, String avatarUrl, String reason) {}
