package com.end.minisbackend.admin.application.model;

public record GrantCreatorRoleCommand(
    String slug, String displayName, String bio, String avatarUrl, String reason) {}
