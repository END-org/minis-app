package com.end.minisbackend.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.end.minisbackend.TestIdentities;
import com.end.minisbackend.identity.application.MinisPrincipal;
import com.end.minisbackend.identity.application.ProvisionedUserAccount;
import com.end.minisbackend.identity.domain.VerifiedFirebaseIdentity;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import com.end.minisbackend.user.domain.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class CurrentUserServiceTest {
  @Test
  void currentUserRequiresProvisionedAccount() {
    UserAccountBootstrapService bootstrapService =
        new UserAccountBootstrapService(
            new UserAccountBootstrapServiceTest.InMemoryUserAccountStore(),
            identity -> UserRole.VIEWER,
            new UserAccountBootstrapServiceTest.TestTransactionManager(),
            fixedClock());
    CurrentUserService service =
        new CurrentUserService(
            new UserAccountBootstrapServiceTest.InMemoryUserAccountStore(),
            new UserAccountBootstrapServiceTest.TestTransactionManager(),
            fixedClock());
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
            store,
            identity -> UserRole.VIEWER,
            new UserAccountBootstrapServiceTest.TestTransactionManager(),
            fixedClock());
    CurrentUserService service =
        new CurrentUserService(
            store, new UserAccountBootstrapServiceTest.TestTransactionManager(), fixedClock());
    VerifiedFirebaseIdentity identity = TestIdentities.googleIdentity("active-user");
    ProvisionedUserAccount bootstrapped = bootstrapService.bootstrap(identity);

    ProvisionedUserAccount current =
        service.currentUser(
            new MinisPrincipal(
                identity, bootstrapped.id(), bootstrapped.role(), bootstrapped.status()));

    assertThat(current.id()).isEqualTo(bootstrapped.id());
  }

  @Test
  void currentUserCanUpdateOnlyProfileFields() {
    UserAccountBootstrapServiceTest.InMemoryUserAccountStore store =
        new UserAccountBootstrapServiceTest.InMemoryUserAccountStore();
    UserAccountBootstrapService bootstrapService =
        new UserAccountBootstrapService(
            store,
            identity -> UserRole.CREATOR,
            new UserAccountBootstrapServiceTest.TestTransactionManager(),
            fixedClock());
    CurrentUserService service =
        new CurrentUserService(
            store, new UserAccountBootstrapServiceTest.TestTransactionManager(), fixedClock());
    VerifiedFirebaseIdentity identity = TestIdentities.googleIdentity("profile-user");
    ProvisionedUserAccount bootstrapped = bootstrapService.bootstrap(identity);

    ProvisionedUserAccount updated =
        service.updateProfile(
            new MinisPrincipal(
                identity, bootstrapped.id(), bootstrapped.role(), bootstrapped.status()),
            new UpdateCurrentProfileCommand(
                "Profile Name", "https://example.com/avatar.png", "Profile bio"));

    assertThat(updated.displayName()).isEqualTo("Profile Name");
    assertThat(updated.avatarUrl()).isEqualTo("https://example.com/avatar.png");
    assertThat(updated.bio()).isEqualTo("Profile bio");
    assertThat(updated.role()).isEqualTo("CREATOR");
    assertThat(updated.status()).isEqualTo("ACTIVE");
  }

  @Test
  void profileUpdateLogsChangedFieldNamesWithoutProfileValues(CapturedOutput output) {
    UserAccountBootstrapServiceTest.InMemoryUserAccountStore store =
        new UserAccountBootstrapServiceTest.InMemoryUserAccountStore();
    UserAccountBootstrapService bootstrapService =
        new UserAccountBootstrapService(
            store,
            identity -> UserRole.VIEWER,
            new UserAccountBootstrapServiceTest.TestTransactionManager(),
            fixedClock());
    CurrentUserService service =
        new CurrentUserService(
            store, new UserAccountBootstrapServiceTest.TestTransactionManager(), fixedClock());
    VerifiedFirebaseIdentity identity = TestIdentities.googleIdentity("profile-log-user");
    ProvisionedUserAccount bootstrapped = bootstrapService.bootstrap(identity);

    service.updateProfile(
        new MinisPrincipal(identity, bootstrapped.id(), bootstrapped.role(), bootstrapped.status()),
        new UpdateCurrentProfileCommand(
            "Private Profile Name", "https://example.com/private-avatar.png", "Private bio"));

    assertThat(output)
        .contains("event=user_profile_updated")
        .contains("changedFields=[displayName, avatarUrl, bio]")
        .doesNotContain("Private Profile Name")
        .doesNotContain("https://example.com/private-avatar.png")
        .doesNotContain("Private bio");
  }

  @Test
  void profileUpdateRejectsInvalidAvatarUrl() {
    UserAccountBootstrapServiceTest.InMemoryUserAccountStore store =
        new UserAccountBootstrapServiceTest.InMemoryUserAccountStore();
    UserAccountBootstrapService bootstrapService =
        new UserAccountBootstrapService(
            store,
            identity -> UserRole.VIEWER,
            new UserAccountBootstrapServiceTest.TestTransactionManager(),
            fixedClock());
    CurrentUserService service =
        new CurrentUserService(
            store, new UserAccountBootstrapServiceTest.TestTransactionManager(), fixedClock());
    VerifiedFirebaseIdentity identity = TestIdentities.googleIdentity("invalid-avatar-user");
    ProvisionedUserAccount bootstrapped = bootstrapService.bootstrap(identity);

    assertThatThrownBy(
            () ->
                service.updateProfile(
                    new MinisPrincipal(
                        identity, bootstrapped.id(), bootstrapped.role(), bootstrapped.status()),
                    new UpdateCurrentProfileCommand("Name", "javascript:alert(1)", null)))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.errorCode()).isEqualTo(ApiErrorCode.VALIDATION_ERROR));
  }

  private static Clock fixedClock() {
    return Clock.fixed(Instant.parse("2026-07-17T12:00:00Z"), ZoneOffset.UTC);
  }
}
