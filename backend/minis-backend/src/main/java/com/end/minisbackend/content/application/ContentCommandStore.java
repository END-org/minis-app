package com.end.minisbackend.content.application;

import com.end.minisbackend.content.domain.ContentItem;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentCommandStore {
  Optional<ContentItem> findByImportSourceKey(String importSourceKey);

  Optional<ContentItem> findById(UUID contentId);

  List<ContentItem> findPublishedItemsAfter(UUID afterContentId, int limit);

  List<ContentItem> findStaleUploadingItemsBefore(Instant startedBefore, int limit);

  ContentItem saveAndFlush(ContentItem contentItem);
}
