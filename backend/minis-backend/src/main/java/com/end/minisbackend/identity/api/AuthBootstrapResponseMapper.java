package com.end.minisbackend.identity.api;

import com.end.minisbackend.identity.application.ProvisionedUserAccount;

final class AuthBootstrapResponseMapper {
  private AuthBootstrapResponseMapper() {}

  static AuthBootstrapResponse toResponse(ProvisionedUserAccount account) {
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
