package com.end.minisbackend.identity.application.port;

import com.end.minisbackend.identity.application.model.ProvisionedUserAccount;
import com.end.minisbackend.identity.domain.model.VerifiedFirebaseIdentity;

/** Creates or synchronizes the local MINIS account for a verified Firebase identity. */
public interface UserAccountProvisioningGateway {
  ProvisionedUserAccount bootstrap(VerifiedFirebaseIdentity identity);
}
