package com.end.minisbackend.admin.api;

import com.end.minisbackend.admin.application.AdminUserPage;
import com.end.minisbackend.admin.application.AdminUserView;
import com.end.minisbackend.creator.application.CreatorProfileView;

final class AdminUserResponseMapper {
  private AdminUserResponseMapper() {}

  static AdminUserResponse toResponse(AdminUserView user) {
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

  static AdminUserListResponse toResponse(AdminUserPage page) {
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
