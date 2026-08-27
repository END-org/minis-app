package com.end.minisbackend.content.application;

import com.end.minisbackend.content.domain.ContentItem;
import com.end.minisbackend.content.domain.ContentStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class InMemoryContentCommandStore implements ContentCommandStore {
  private final Map<UUID, ContentItem> byId = new ConcurrentHashMap<>();
  private final Map<String, ContentItem> bySourceKey = new ConcurrentHashMap<>();
  private boolean failNextSave;

  @Override
  public Optional<ContentItem> findByImportSourceKey(String importSourceKey) {
    return Optional.ofNullable(bySourceKey.get(importSourceKey));
  }

  @Override
  public Optional<ContentItem> findById(UUID contentId) {
    return Optional.ofNullable(byId.get(contentId));
  }

  @Override
  public List<ContentItem> findPublishedItems() {
    return byId.values().stream().filter(item -> item.status() == ContentStatus.PUBLISHED).toList();
  }

  @Override
  public ContentItem saveAndFlush(ContentItem contentItem) {
    if (failNextSave) {
      failNextSave = false;
      throw new org.springframework.dao.DataIntegrityViolationException("simulated failure");
    }
    byId.put(contentItem.id(), contentItem);
    bySourceKey.put(contentItem.importSourceKey(), contentItem);
    return contentItem;
  }

  List<ContentItem> all() {
    return new ArrayList<>(byId.values());
  }

  void failNextSave() {
    failNextSave = true;
  }
}
