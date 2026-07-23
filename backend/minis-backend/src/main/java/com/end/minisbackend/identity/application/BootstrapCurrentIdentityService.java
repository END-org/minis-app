package com.end.minisbackend.identity.application;

import com.end.minisbackend.identity.domain.VerifiedFirebaseIdentity;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import org.springframework.stereotype.Service;

/** Coordinates the authentication bootstrap use case without owning persistence details. */
@Service
public class BootstrapCurrentIdentityService {
  private final UserAccountProvisioningGateway userAccounts;

  public BootstrapCurrentIdentityService(UserAccountProvisioningGateway userAccounts) {
    this.userAccounts = userAccounts;
  }

  public ProvisionedUserAccount bootstrap(VerifiedFirebaseIdentity identity) {
    if (identity.isEmailPasswordSignIn() && !identity.emailVerified()) {
      throw new ApiException(ApiErrorCode.EMAIL_NOT_VERIFIED);
    }
    return userAccounts.bootstrap(identity);
  }
}
