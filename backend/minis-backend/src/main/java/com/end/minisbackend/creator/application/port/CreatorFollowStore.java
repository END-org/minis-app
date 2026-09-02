package com.end.minisbackend.creator.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface CreatorFollowStore {
  int ensureFollowIfCreatorIsPublic(UUID followerUserId, UUID creatorUserId, Instant createdAt);

  boolean isFollowing(UUID followerUserId, UUID creatorUserId);

  void removeFollow(UUID followerUserId, UUID creatorUserId);

  Optional<UUID> findCreatorUserIdBySlug(String slug);

  Set<UUID> followedCreatorIds(UUID followerUserId, List<UUID> creatorUserIds);
}
