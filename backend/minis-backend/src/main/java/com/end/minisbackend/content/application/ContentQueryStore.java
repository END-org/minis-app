package com.end.minisbackend.content.application;

import com.end.minisbackend.content.domain.ContentCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentQueryStore {
  List<PublishedContentView> findFeed(ContentCategory category, ContentCursor cursor, int limit);

  List<PublishedContentView> findCreatorContent(
      UUID creatorUserId, ContentCursor cursor, int limit);

  Optional<PublishedContentView> findPublishedById(UUID contentId);
}
