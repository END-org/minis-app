package com.end.minisbackend.identity.application;

import com.end.minisbackend.identity.domain.VerifiedFirebaseIdentity;

public interface BootstrapAuthenticationContextRefresher {
  void refresh(VerifiedFirebaseIdentity identity, ProvisionedUserAccount account);
}
