package com.end.minisbackend.identity.application.port;

import com.end.minisbackend.identity.application.model.ProvisionedUserAccount;
import com.end.minisbackend.identity.domain.model.VerifiedFirebaseIdentity;

public interface BootstrapAuthenticationContextRefresher {
  void refresh(VerifiedFirebaseIdentity identity, ProvisionedUserAccount account);
}
