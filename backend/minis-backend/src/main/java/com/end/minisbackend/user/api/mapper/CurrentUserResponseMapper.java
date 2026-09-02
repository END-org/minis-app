package com.end.minisbackend.user.api.mapper;

import com.end.minisbackend.identity.application.model.ProvisionedUserAccount;
import com.end.minisbackend.user.api.dto.CurrentUserResponse;

public final class CurrentUserResponseMapper {
  private CurrentUserResponseMapper() {}

  public static CurrentUserResponse toResponse(ProvisionedUserAccount account) {
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
