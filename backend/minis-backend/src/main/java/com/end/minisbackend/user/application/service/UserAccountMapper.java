package com.end.minisbackend.user.application.service;

import com.end.minisbackend.identity.application.model.LocalUserIdentity;
import com.end.minisbackend.identity.application.model.ProvisionedUserAccount;
import com.end.minisbackend.user.domain.model.UserAccount;

final class UserAccountMapper {
  private UserAccountMapper() {}

  static ProvisionedUserAccount toProvisionedAccount(UserAccount account) {
    return new ProvisionedUserAccount(
        account.id(),
        account.email(),
        account.emailVerified(),
        account.displayName(),
        account.avatarUrl(),
        account.bio(),
        account.role().name(),
        account.status().name(),
        account.createdAt());
  }

  static LocalUserIdentity toLocalIdentity(UserAccount account) {
    return new LocalUserIdentity(account.id(), account.role().name(), account.status().name());
  }
}
