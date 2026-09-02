package com.end.minisbackend.creator.api;

import com.end.minisbackend.creator.application.CreatorDirectoryPage;
import com.end.minisbackend.creator.application.CreatorDirectoryView;
import com.end.minisbackend.creator.application.CreatorProfileItem;
import com.end.minisbackend.creator.application.CreatorProfileView;

final class CreatorResponseMapper {
  private CreatorResponseMapper() {}

  static CreatorResponse toResponse(CreatorProfileView profile) {
    return toResponse(new CreatorProfileItem(profile, false));
  }

  static CreatorResponse toResponse(CreatorProfileItem item) {
    CreatorProfileView profile = item.profile();
    return new CreatorResponse(
        profile.userId(),
        profile.slug(),
        profile.displayName(),
        profile.bio(),
        profile.avatarUrl(),
        profile.verified(),
        item.following(),
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

  static CreatorDirectoryResponse toResponse(CreatorDirectoryView page) {
    return new CreatorDirectoryResponse(
        page.creators().stream().map(CreatorResponseMapper::toResponse).toList(),
        page.page(),
        page.size(),
        page.hasNext());
  }
}
