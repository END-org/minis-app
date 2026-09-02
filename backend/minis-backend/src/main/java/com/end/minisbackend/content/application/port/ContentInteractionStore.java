package com.end.minisbackend.content.application.port;

import com.end.minisbackend.content.application.model.feed.PersonalContentView;
import com.end.minisbackend.content.application.model.interaction.ContentInteractionCursor;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface ContentInteractionStore {
  int ensureLikedIfPublished(UUID userId, UUID contentId, Instant createdAt);

  boolean isLiked(UUID userId, UUID contentId);

  void removeLike(UUID userId, UUID contentId);

  int ensureSavedIfPublished(UUID userId, UUID contentId, Instant createdAt);

  boolean isSaved(UUID userId, UUID contentId);

  void removeSave(UUID userId, UUID contentId);

  boolean publishedContentExists(UUID contentId);

  Map<UUID, Long> likeCountsByContentIds(List<UUID> contentIds);

  Set<UUID> likedContentIds(UUID userId, List<UUID> contentIds);

  Set<UUID> savedContentIds(UUID userId, List<UUID> contentIds);

  List<PersonalContentView> findLikedContent(
      UUID userId, ContentInteractionCursor cursor, int limit);

  List<PersonalContentView> findSavedContent(
      UUID userId, ContentInteractionCursor cursor, int limit);
}
