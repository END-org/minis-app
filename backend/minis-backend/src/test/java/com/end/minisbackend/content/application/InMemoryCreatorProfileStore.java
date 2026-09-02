package com.end.minisbackend.content.application;

import com.end.minisbackend.creator.application.model.CreatorDirectoryPage;
import com.end.minisbackend.creator.application.model.CreatorProfileView;
import com.end.minisbackend.creator.application.port.CreatorProfileStore;
import com.end.minisbackend.creator.domain.model.CreatorProfile;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class InMemoryCreatorProfileStore implements CreatorProfileStore {
  private final CreatorProfileView creator;

  InMemoryCreatorProfileStore(UUID userId, String slug) {
    this.creator =
        new CreatorProfileView(
            userId,
            slug,
            "Event Creator",
            null,
            null,
            true,
            Instant.parse("2026-07-17T12:00:00Z"),
            Instant.parse("2026-07-17T12:00:00Z"));
  }

  @Override
  public Optional<CreatorProfile> findByUserId(UUID userId) {
    return Optional.empty();
  }

  @Override
  public Optional<CreatorProfileView> findPublicBySlug(String slug) {
    return creator.slug().equals(slug) ? Optional.of(creator) : Optional.empty();
  }

  @Override
  public Optional<CreatorProfileView> findPublicByUserId(UUID userId) {
    return creator.userId().equals(userId) ? Optional.of(creator) : Optional.empty();
  }

  @Override
  public CreatorDirectoryPage findPublicCreators(int page, int size) {
    return new CreatorDirectoryPage(List.of(creator), page, size, false);
  }

  @Override
  public List<CreatorProfileView> findByUserIds(List<UUID> userIds) {
    return userIds.contains(creator.userId()) ? List.of(creator) : List.of();
  }

  @Override
  public CreatorProfile saveAndFlush(CreatorProfile profile) {
    return profile;
  }
}
