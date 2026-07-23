package com.end.minisbackend.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.end.minisbackend.TestIdentities;
import com.end.minisbackend.identity.application.MinisPrincipal;
import com.end.minisbackend.identity.application.ProvisionedUserAccount;
import com.end.minisbackend.identity.domain.VerifiedFirebaseIdentity;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CurrentUserServiceTest {
  @Test
  void currentUserRequiresProvisionedAccount() {
    UserAccountBootstrapService bootstrapService =
        new UserAccountBootstrapService(
            new UserAccountBootstrapServiceTest.InMemoryUserAccountStore(),
            new UserAccountBootstrapServiceTest.TestTransactionManager());
    CurrentUserService service =
        new CurrentUserService(
            new UserAccountBootstrapServiceTest.InMemoryUserAccountStore(),
            new UserAccountBootstrapServiceTest.TestTransactionManager());
    VerifiedFirebaseIdentity identity = TestIdentities.googleIdentity("prebootstrap-user");

    assertThat(bootstrapService.findByFirebaseUid(identity.firebaseUid()))
        .isEqualTo(Optional.empty());
    assertThatThrownBy(() -> service.currentUser(new MinisPrincipal(identity, null, null, null)))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.errorCode()).isEqualTo(ApiErrorCode.ACCOUNT_NOT_PROVISIONED));
  }

  @Test
  void currentUserReturnsActiveProvisionedAccount() {
    UserAccountBootstrapServiceTest.InMemoryUserAccountStore store =
        new UserAccountBootstrapServiceTest.InMemoryUserAccountStore();
    UserAccountBootstrapService bootstrapService =
        new UserAccountBootstrapService(
            store, new UserAccountBootstrapServiceTest.TestTransactionManager());
    CurrentUserService service =
        new CurrentUserService(store, new UserAccountBootstrapServiceTest.TestTransactionManager());
    VerifiedFirebaseIdentity identity = TestIdentities.googleIdentity("active-user");
    ProvisionedUserAccount bootstrapped = bootstrapService.bootstrap(identity);

    ProvisionedUserAccount current =
        service.currentUser(
            new MinisPrincipal(
                identity, bootstrapped.id(), bootstrapped.role(), bootstrapped.status()));

    assertThat(current.id()).isEqualTo(bootstrapped.id());
  }
}
