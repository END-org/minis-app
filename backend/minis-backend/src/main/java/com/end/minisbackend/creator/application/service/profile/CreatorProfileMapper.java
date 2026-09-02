package com.end.minisbackend.creator.application.service.profile;

import com.end.minisbackend.creator.application.model.CreatorProfileView;
import com.end.minisbackend.creator.domain.model.CreatorProfile;

final class CreatorProfileMapper {
  private CreatorProfileMapper() {}

  static CreatorProfileView toView(CreatorProfile profile) {
    return new CreatorProfileView(
        profile.userId(),
        profile.publicSlug(),
        profile.publicDisplayName(),
        profile.bio(),
        profile.avatarUrl(),
        profile.verified(),
        profile.createdAt(),
        profile.updatedAt());
  }
}
