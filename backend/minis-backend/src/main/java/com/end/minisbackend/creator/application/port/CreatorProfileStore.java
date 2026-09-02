package com.end.minisbackend.creator.application.port;

import com.end.minisbackend.creator.application.model.CreatorDirectoryPage;
import com.end.minisbackend.creator.application.model.CreatorProfileView;
import com.end.minisbackend.creator.domain.model.CreatorProfile;
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
