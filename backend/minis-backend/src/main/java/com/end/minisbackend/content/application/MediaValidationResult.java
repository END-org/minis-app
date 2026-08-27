package com.end.minisbackend.content.application;

public record MediaValidationResult(String contentType, long sizeBytes, Long durationMs) {}
