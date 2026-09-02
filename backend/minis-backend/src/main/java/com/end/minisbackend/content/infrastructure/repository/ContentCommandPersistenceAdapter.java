package com.end.minisbackend.content.infrastructure.repository;

import com.end.minisbackend.content.application.port.ContentCommandStore;
import com.end.minisbackend.content.domain.enums.ContentStatus;
import com.end.minisbackend.content.domain.model.ContentItem;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
class ContentCommandPersistenceAdapter implements ContentCommandStore {
  private final ContentItemRepository repository;

  ContentCommandPersistenceAdapter(ContentItemRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<ContentItem> findByImportSourceKey(String importSourceKey) {
    return repository.findByImportSourceKey(importSourceKey);
  }

  @Override
  public Optional<ContentItem> findById(UUID contentId) {
    return repository.findById(contentId);
  }

  @Override
  public List<ContentItem> findPublishedItemsAfter(UUID afterContentId, int limit) {
    PageRequest page = PageRequest.of(0, limit);
    if (afterContentId == null) {
      return repository.findByStatusOrderByIdAsc(ContentStatus.PUBLISHED, page);
    }
    return repository.findByStatusAndIdGreaterThanOrderByIdAsc(
        ContentStatus.PUBLISHED, afterContentId, page);
  }

  @Override
  public List<ContentItem> findStaleUploadingItemsBefore(Instant startedBefore, int limit) {
    return repository.findByStatusAndUploadStartedAtBeforeOrderByUploadStartedAtAscIdAsc(
        ContentStatus.UPLOADING, startedBefore, PageRequest.of(0, limit));
  }

  @Override
  public ContentItem saveAndFlush(ContentItem contentItem) {
    return repository.saveAndFlush(contentItem);
  }
}
