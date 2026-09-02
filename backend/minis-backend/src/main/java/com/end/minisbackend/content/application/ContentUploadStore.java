package com.end.minisbackend.content.application;

import com.end.minisbackend.content.domain.ContentItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentUploadStore {
  Optional<ContentItem> findById(UUID contentId);

  Optional<ContentItem> findByCreatorUserIdAndUploadIdempotencyKey(
      UUID creatorUserId, String uploadIdempotencyKey);

  Optional<ContentUploadView> findUploadByCreatorUserIdAndId(UUID creatorUserId, UUID contentId);

  List<ContentUploadView> findUploadsByCreatorUserId(
      UUID creatorUserId, ContentUploadCursor cursor, int limit);

  ContentItem saveAndFlush(ContentItem contentItem);

  boolean publishedContentOwnsObject(UUID contentId, String objectKey);
}
