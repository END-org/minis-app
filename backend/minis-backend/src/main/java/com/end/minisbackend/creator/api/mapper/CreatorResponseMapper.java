package com.end.minisbackend.creator.api.mapper;

import com.end.minisbackend.creator.api.dto.CreatorDirectoryResponse;
import com.end.minisbackend.creator.api.dto.CreatorResponse;
import com.end.minisbackend.creator.application.model.CreatorDirectoryPage;
import com.end.minisbackend.creator.application.model.CreatorDirectoryView;
import com.end.minisbackend.creator.application.model.CreatorProfileItem;
import com.end.minisbackend.creator.application.model.CreatorProfileView;

public final class CreatorResponseMapper {
  private CreatorResponseMapper() {}

  public static CreatorResponse toResponse(CreatorProfileView profile) {
    return toResponse(new CreatorProfileItem(profile, false));
  }

  public static CreatorResponse toResponse(CreatorProfileItem item) {
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

  public static CreatorDirectoryResponse toResponse(CreatorDirectoryPage page) {
    return new CreatorDirectoryResponse(
        page.creators().stream().map(CreatorResponseMapper::toResponse).toList(),
        page.page(),
        page.size(),
        page.hasNext());
  }

  public static CreatorDirectoryResponse toResponse(CreatorDirectoryView page) {
    return new CreatorDirectoryResponse(
        page.creators().stream().map(CreatorResponseMapper::toResponse).toList(),
        page.page(),
        page.size(),
        page.hasNext());
  }
}
