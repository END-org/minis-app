package com.end.minisbackend.content.application.model.upload;

import com.end.minisbackend.content.application.port.UploadVideoSource;

public record UploadContentCommand(
    String idempotencyKey,
    String title,
    String description,
    String category,
    UploadVideoSource video) {}
