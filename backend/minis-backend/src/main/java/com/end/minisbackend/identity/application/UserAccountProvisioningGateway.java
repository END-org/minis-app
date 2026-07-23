package com.end.minisbackend.identity.application;

import com.end.minisbackend.identity.domain.VerifiedFirebaseIdentity;

/** Creates or synchronizes the local MINIS account for a verified Firebase identity. */
public interface UserAccountProvisioningGateway {
  ProvisionedUserAccount bootstrap(VerifiedFirebaseIdentity identity);
}
