package com.end.minisbackend.creator.api;

import com.end.minisbackend.creator.application.CreatorDirectoryPage;
import com.end.minisbackend.creator.application.CreatorProfileView;

final class CreatorResponseMapper {
  private CreatorResponseMapper() {}

  static CreatorResponse toResponse(CreatorProfileView profile) {
    return new CreatorResponse(
        profile.userId(),
        profile.slug(),
        profile.displayName(),
        profile.bio(),
        profile.avatarUrl(),
        profile.verified(),
        profile.createdAt(),
        profile.updatedAt());
  }

  static CreatorDirectoryResponse toResponse(CreatorDirectoryPage page) {
    return new CreatorDirectoryResponse(
        page.creators().stream().map(CreatorResponseMapper::toResponse).toList(),
        page.page(),
        page.size(),
        page.hasNext());
  }
}
