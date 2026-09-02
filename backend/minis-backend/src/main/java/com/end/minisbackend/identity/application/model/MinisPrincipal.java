package com.end.minisbackend.identity.application.model;

import com.end.minisbackend.identity.domain.model.VerifiedFirebaseIdentity;
import java.io.Serializable;
import java.util.UUID;

/** Authenticated caller identity, including the optional provisioned MINIS account. */
public record MinisPrincipal(
    VerifiedFirebaseIdentity firebaseIdentity, UUID userId, String role, String accountStatus)
    implements Serializable {
  private static final long serialVersionUID = 1L;
  private static final String ACTIVE_STATUS = "ACTIVE";

  public boolean hasProvisionedAccount() {
    return userId != null;
  }

  public boolean isActiveProvisionedAccount() {
    return hasProvisionedAccount() && ACTIVE_STATUS.equals(accountStatus);
  }
}
