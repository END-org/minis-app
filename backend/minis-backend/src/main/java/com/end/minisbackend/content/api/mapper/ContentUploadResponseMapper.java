package com.end.minisbackend.content.api.mapper;

import com.end.minisbackend.content.api.dto.upload.ContentUploadMediaResponse;
import com.end.minisbackend.content.api.dto.upload.ContentUploadPageResponse;
import com.end.minisbackend.content.api.dto.upload.ContentUploadResponse;
import com.end.minisbackend.content.application.model.upload.ContentUploadItem;
import com.end.minisbackend.content.application.model.upload.ContentUploadPage;

public final class ContentUploadResponseMapper {
  private ContentUploadResponseMapper() {}

  public static ContentUploadPageResponse toResponse(ContentUploadPage page) {
    return new ContentUploadPageResponse(
        page.items().stream().map(ContentUploadResponseMapper::toResponse).toList(),
        page.nextCursor(),
        page.size());
  }

  public static ContentUploadResponse toResponse(ContentUploadItem item) {
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
