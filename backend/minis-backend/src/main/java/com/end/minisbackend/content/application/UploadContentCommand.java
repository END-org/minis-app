package com.end.minisbackend.content.application;

public record UploadContentCommand(
    String idempotencyKey,
    String title,
    String description,
    String category,
    UploadVideoSource video) {}
