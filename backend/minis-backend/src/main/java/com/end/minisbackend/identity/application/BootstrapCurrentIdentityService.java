package com.end.minisbackend.identity.application;

import com.end.minisbackend.identity.domain.VerifiedFirebaseIdentity;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import com.end.minisbackend.shared.observability.SafeIdentityFingerprint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/** Coordinates the authentication bootstrap use case without owning persistence details. */
@Service
public class BootstrapCurrentIdentityService {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(BootstrapCurrentIdentityService.class);

  private final UserAccountProvisioningGateway userAccounts;

  public BootstrapCurrentIdentityService(UserAccountProvisioningGateway userAccounts) {
    this.userAccounts = userAccounts;
  }

  public ProvisionedUserAccount bootstrap(VerifiedFirebaseIdentity identity) {
    if (identity.isEmailPasswordSignIn() && !identity.emailVerified()) {
      LOGGER.warn(
          "event=user_bootstrap_rejected correlationId={} firebaseUidFingerprint={} provider={} "
              + "apiErrorCode={}",
          valueOrEmpty(MDC.get("correlationId")),
          SafeIdentityFingerprint.firebaseUid(identity.firebaseUid()),
          valueOrEmpty(identity.signInProvider()),
          ApiErrorCode.EMAIL_NOT_VERIFIED.name());
      throw new ApiException(ApiErrorCode.EMAIL_NOT_VERIFIED);
    }
    return userAccounts.bootstrap(identity);
  }

  private static String valueOrEmpty(String value) {
    return value == null || value.isBlank() ? "-" : value;
  }
}
