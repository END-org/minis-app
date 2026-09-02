package com.end.minisbackend.content.domain.model.media;

public record MediaInspection(
    String container,
    String videoCodec,
    String audioCodec,
    long durationMs,
    int width,
    int height,
    Double frameRate,
    int streamCount) {}
