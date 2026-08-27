package com.end.minisbackend.content.application;

public record MediaInspection(
    String container,
    String videoCodec,
    String audioCodec,
    long durationMs,
    int width,
    int height,
    Double frameRate,
    int streamCount) {}
