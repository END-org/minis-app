package com.end.minisbackend.identity.api.mapper;

import com.end.minisbackend.identity.api.dto.AuthBootstrapResponse;
import com.end.minisbackend.identity.application.model.ProvisionedUserAccount;

public final class AuthBootstrapResponseMapper {
  private AuthBootstrapResponseMapper() {}

  public static AuthBootstrapResponse toResponse(ProvisionedUserAccount account) {
    return new AuthBootstrapResponse(
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
