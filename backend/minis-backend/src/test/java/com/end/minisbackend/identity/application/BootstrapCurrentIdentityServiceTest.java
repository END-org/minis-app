package com.end.minisbackend.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.end.minisbackend.TestIdentities;
import com.end.minisbackend.identity.domain.VerifiedFirebaseIdentity;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BootstrapCurrentIdentityServiceTest {
  @Test
  void rejectsUnverifiedEmailPasswordIdentity() {
    BootstrapCurrentIdentityService service =
        new BootstrapCurrentIdentityService(identity -> provisionedAccount());

    assertThatThrownBy(
            () -> service.bootstrap(TestIdentities.emailPasswordIdentity("email-user", false)))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.errorCode()).isEqualTo(ApiErrorCode.EMAIL_NOT_VERIFIED));
  }

  @Test
  void allowsFederatedIdentityWithoutEmailPasswordVerificationGate() {
    RecordingGateway gateway = new RecordingGateway();
    BootstrapCurrentIdentityService service = new BootstrapCurrentIdentityService(gateway);

    ProvisionedUserAccount response =
        service.bootstrap(
            new VerifiedFirebaseIdentity(
                "google-user",
                "google-user@example.com",
                false,
                "Google User",
                null,
                "google.com",
                Instant.parse("2026-07-17T12:00:00Z"),
                Instant.parse("2026-07-17T13:00:00Z")));

    assertThat(response.id()).isNotNull();
    assertThat(gateway.called).isTrue();
  }

  private static ProvisionedUserAccount provisionedAccount() {
    return new ProvisionedUserAccount(
        UUID.randomUUID(),
        "user@example.com",
        true,
        "User",
        null,
        null,
        "VIEWER",
        "ACTIVE",
        Instant.parse("2026-07-17T12:00:00Z"));
  }

  private static final class RecordingGateway implements UserAccountProvisioningGateway {
    private boolean called;

    @Override
    public ProvisionedUserAccount bootstrap(VerifiedFirebaseIdentity identity) {
      called = true;
      return provisionedAccount();
    }
  }
}
