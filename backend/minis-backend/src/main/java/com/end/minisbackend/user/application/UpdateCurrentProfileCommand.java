package com.end.minisbackend.user.application;

public record UpdateCurrentProfileCommand(String displayName, String avatarUrl, String bio) {}
