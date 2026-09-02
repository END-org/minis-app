package com.end.minisbackend.content.domain.model.media;

public record MediaValidationResult(String contentType, long sizeBytes, Long durationMs) {}
