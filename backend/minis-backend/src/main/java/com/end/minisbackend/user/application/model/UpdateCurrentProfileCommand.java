package com.end.minisbackend.user.application.model;

public record UpdateCurrentProfileCommand(String displayName, String avatarUrl, String bio) {}
