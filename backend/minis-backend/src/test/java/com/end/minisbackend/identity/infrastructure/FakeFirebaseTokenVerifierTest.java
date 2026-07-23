package com.end.minisbackend.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.end.minisbackend.TestIdentities;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.security.MinisAuthenticationException;
import org.junit.jupiter.api.Test;

class FakeFirebaseTokenVerifierTest {
  @Test
  void returnsRegisteredIdentity() {
    FakeFirebaseTokenVerifier verifier = new FakeFirebaseTokenVerifier();
    verifier.register("known", TestIdentities.googleIdentity("known-user"));

    assertThat(verifier.verify("known").firebaseUid()).isEqualTo("known-user");
  }

  @Test
  void mapsUnknownTokenToInvalidAuthenticationFailure() {
    FakeFirebaseTokenVerifier verifier = new FakeFirebaseTokenVerifier();

    assertThatThrownBy(() -> verifier.verify("missing"))
        .isInstanceOfSatisfying(
            MinisAuthenticationException.class,
            exception ->
                assertThat(exception.errorCode()).isEqualTo(ApiErrorCode.INVALID_AUTH_TOKEN));
  }
}
