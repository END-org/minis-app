package com.end.minisbackend.creator.application;

import com.end.minisbackend.creator.domain.CreatorProfile;

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
