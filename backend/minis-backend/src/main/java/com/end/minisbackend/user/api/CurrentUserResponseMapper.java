package com.end.minisbackend.user.api;

import com.end.minisbackend.identity.application.ProvisionedUserAccount;

final class CurrentUserResponseMapper {
  private CurrentUserResponseMapper() {}

  static CurrentUserResponse toResponse(ProvisionedUserAccount account) {
    return new CurrentUserResponse(
        account.id(),
        account.email(),
        account.emailVerified(),
        account.displayName(),
        account.avatarUrl(),
        account.bio(),
        account.role(),
        account.status(),
        account.createdAt());
  }
}
