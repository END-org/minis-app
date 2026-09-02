package com.end.minisbackend.content.application;

import com.end.minisbackend.creator.application.CreatorFollowStore;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

final class NoopCreatorFollowStore implements CreatorFollowStore {
  @Override
  public int ensureFollowIfCreatorIsPublic(
      UUID followerUserId, UUID creatorUserId, Instant createdAt) {
    return 0;
  }

  @Override
  public boolean isFollowing(UUID followerUserId, UUID creatorUserId) {
    return false;
  }

  @Override
  public void removeFollow(UUID followerUserId, UUID creatorUserId) {}

  @Override
  public Optional<UUID> findCreatorUserIdBySlug(String slug) {
    return Optional.empty();
  }

  @Override
  public Set<UUID> followedCreatorIds(UUID followerUserId, List<UUID> creatorUserIds) {
    return Set.of();
  }
}
