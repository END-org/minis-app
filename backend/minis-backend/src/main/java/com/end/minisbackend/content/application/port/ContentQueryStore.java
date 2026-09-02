package com.end.minisbackend.content.application.port;

import com.end.minisbackend.content.application.model.feed.ContentCursor;
import com.end.minisbackend.content.application.model.feed.PublishedContentView;
import com.end.minisbackend.content.domain.enums.ContentCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentQueryStore {
  List<PublishedContentView> findFeed(ContentCategory category, ContentCursor cursor, int limit);

  List<PublishedContentView> findCreatorContent(
      UUID creatorUserId, ContentCursor cursor, int limit);

  Optional<PublishedContentView> findPublishedById(UUID contentId);
}
