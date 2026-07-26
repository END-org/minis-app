package com.end.minisbackend.creator.infrastructure;

import com.end.minisbackend.creator.domain.CreatorProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CreatorProfileRepository extends JpaRepository<CreatorProfile, UUID> {
  Optional<CreatorProfile> findByUserId(UUID userId);

  List<CreatorProfile> findByUserIdIn(List<UUID> userIds);
}
