package com.end.minisbackend.admin.api.mapper;

import com.end.minisbackend.admin.api.dto.AdminCreatorProfileResponse;
import com.end.minisbackend.admin.api.dto.AdminUserListResponse;
import com.end.minisbackend.admin.api.dto.AdminUserResponse;
import com.end.minisbackend.admin.application.model.AdminUserPage;
import com.end.minisbackend.admin.application.model.AdminUserView;
import com.end.minisbackend.creator.application.model.CreatorProfileView;

public final class AdminUserResponseMapper {
  private AdminUserResponseMapper() {}

  public static AdminUserResponse toResponse(AdminUserView user) {
    return new AdminUserResponse(
        user.id(),
        user.email(),
        user.displayName(),
        user.avatarUrl(),
        user.bio(),
        user.role(),
        user.status(),
        user.createdAt(),
        user.updatedAt(),
        toCreatorResponse(user.creatorProfile()));
  }

  public static AdminUserListResponse toResponse(AdminUserPage page) {
    return new AdminUserListResponse(
        page.users().stream().map(AdminUserResponseMapper::toResponse).toList(),
        page.page(),
        page.size(),
        page.hasNext());
  }

  private static AdminCreatorProfileResponse toCreatorResponse(CreatorProfileView profile) {
    if (profile == null) {
      return null;
    }
    return new AdminCreatorProfileResponse(
        profile.userId(),
        profile.slug(),
        profile.displayName(),
        profile.bio(),
        profile.avatarUrl(),
        profile.verified(),
        profile.createdAt(),
        profile.updatedAt());
  }
}
