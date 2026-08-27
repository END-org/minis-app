package com.end.minisbackend.content.application;

import com.end.minisbackend.content.domain.ContentItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentCommandStore {
  Optional<ContentItem> findByImportSourceKey(String importSourceKey);

  Optional<ContentItem> findById(UUID contentId);

  List<ContentItem> findPublishedItems();

  ContentItem saveAndFlush(ContentItem contentItem);
}
