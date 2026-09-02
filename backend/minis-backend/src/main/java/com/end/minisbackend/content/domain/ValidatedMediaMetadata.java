package com.end.minisbackend.content.domain;

public record ValidatedMediaMetadata(String contentType, long sizeBytes, Long durationMs) {}
