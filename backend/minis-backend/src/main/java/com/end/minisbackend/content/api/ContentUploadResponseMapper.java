package com.end.minisbackend.content.api;

import com.end.minisbackend.content.application.ContentUploadItem;
import com.end.minisbackend.content.application.ContentUploadPage;

final class ContentUploadResponseMapper {
  private ContentUploadResponseMapper() {}

  static ContentUploadPageResponse toResponse(ContentUploadPage page) {
    return new ContentUploadPageResponse(
        page.items().stream().map(ContentUploadResponseMapper::toResponse).toList(),
        page.nextCursor(),
        page.size());
  }

  static ContentUploadResponse toResponse(ContentUploadItem item) {
    return new ContentUploadResponse(
        item.id(),
        item.title(),
        item.description(),
        item.category(),
        item.categoryLabel(),
        item.status(),
        item.failureCode(),
        item.failureReason(),
        item.createdAt(),
        item.uploadStartedAt(),
        item.uploadCompletedAt(),
        item.publishedAt(),
        new ContentUploadMediaResponse(
            item.mediaContentType(), item.mediaSizeBytes(), item.durationMs()),
        item.content() == null ? null : ContentResponseMapper.toResponse(item.content()));
  }
}
