package com.end.minisbackend.creator.application;

import com.end.minisbackend.creator.domain.CreatorProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreatorProfileStore {
  Optional<CreatorProfile> findByUserId(UUID userId);

  Optional<CreatorProfileView> findPublicBySlug(String slug);

  Optional<CreatorProfileView> findPublicByUserId(UUID userId);

  CreatorDirectoryPage findPublicCreators(int page, int size);

  List<CreatorProfileView> findByUserIds(List<UUID> userIds);

  CreatorProfile saveAndFlush(CreatorProfile profile);
}
