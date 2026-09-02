package com.end.minisbackend.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.end.minisbackend.TestIdentities;
import com.end.minisbackend.identity.application.model.ProvisionedUserAccount;
import com.end.minisbackend.identity.domain.model.VerifiedFirebaseIdentity;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import com.end.minisbackend.user.application.port.InitialUserRolePolicy;
import com.end.minisbackend.user.application.port.UserAccountStore;
import com.end.minisbackend.user.domain.enums.AccountStatus;
import com.end.minisbackend.user.domain.enums.UserRole;
import com.end.minisbackend.user.domain.model.UserAccount;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

@ExtendWith(OutputCaptureExtension.class)
class UserAccountBootstrapServiceTest {
  @Test
  void bootstrapCreatesViewerActiveUser() {
    InMemoryUserAccountStore store = new InMemoryUserAccountStore();
    UserAccountBootstrapService service = service(store);

    ProvisionedUserAccount account =
        service.bootstrap(TestIdentities.emailPasswordIdentity("new-user", true));

    assertThat(account.id()).isNotNull();
    assertThat(account.role()).isEqualTo("VIEWER");
    assertThat(account.status()).isEqualTo("ACTIVE");
    assertThat(store.countByFirebaseUid("new-user")).isEqualTo(1);
  }

  @Test
  void unverifiedPasswordIdentityCannotBootstrap() {
    InMemoryUserAccountStore store = new InMemoryUserAccountStore();
    UserAccountBootstrapService service = service(store);

    assertThatThrownBy(
            () -> service.bootstrap(TestIdentities.emailPasswordIdentity("unverified-user", false)))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.errorCode()).isEqualTo(ApiErrorCode.EMAIL_NOT_VERIFIED));
    assertThat(store.countByFirebaseUid("unverified-user")).isZero();
  }

  @Test
  void googleIdentityCanBootstrapWithoutPasswordVerificationGate() {
    InMemoryUserAccountStore store = new InMemoryUserAccountStore();
    UserAccountBootstrapService service = service(store);

    ProvisionedUserAccount account =
        service.bootstrap(TestIdentities.googleIdentity("google-user"));

    assertThat(account.role()).isEqualTo("VIEWER");
    assertThat(account.emailVerified()).isTrue();
    assertThat(store.countByFirebaseUid("google-user")).isEqualTo(1);
  }

  @Test
  void bootstrapCanPersistConfiguredInitialAdminRole() {
    InMemoryUserAccountStore store = new InMemoryUserAccountStore();
    UserAccountBootstrapService service = service(store, allowlistedAdminPolicy("event-admin"));

    ProvisionedUserAccount account =
        service.bootstrap(TestIdentities.emailPasswordIdentity("event-admin", true));

    assertThat(account.role()).isEqualTo("ADMIN");
    assertThat(store.countByFirebaseUid("event-admin")).isEqualTo(1);
  }

  @Test
  void existingViewerAllowlistedForEventAdminBootstrapPromotesToAdmin(CapturedOutput output) {
    InMemoryUserAccountStore store = new InMemoryUserAccountStore();
    VerifiedFirebaseIdentity identity =
        TestIdentities.emailPasswordIdentity("existing-viewer", true);
    ProvisionedUserAccount created = service(store).bootstrap(identity);
    assertThat(created.role()).isEqualTo("VIEWER");

    ProvisionedUserAccount promoted =
        service(store, allowlistedAdminPolicy("existing-viewer")).bootstrap(identity);

    assertThat(promoted.id()).isEqualTo(created.id());
    assertThat(promoted.role()).isEqualTo("ADMIN");
    assertThat(store.findById(created.id()).orElseThrow().role()).isEqualTo(UserRole.ADMIN);
    assertThat(output)
        .contains("event=user_bootstrap_existing_promoted_to_admin")
        .contains("role=ADMIN")
        .doesNotContain("existing-viewer")
        .doesNotContain("existing-viewer@example.com");
  }

  @Test
  void existingCreatorAllowlistedForEventAdminBootstrapPromotesToAdmin() {
    InMemoryUserAccountStore store = new InMemoryUserAccountStore();
    VerifiedFirebaseIdentity identity =
        TestIdentities.emailPasswordIdentity("existing-creator", true);
    ProvisionedUserAccount created = service(store).bootstrap(identity);
    store.findById(created.id()).orElseThrow().grantCreator(Instant.now());

    ProvisionedUserAccount promoted =
        service(store, allowlistedAdminPolicy("existing-creator")).bootstrap(identity);

    assertThat(promoted.id()).isEqualTo(created.id());
    assertThat(promoted.role()).isEqualTo("ADMIN");
    assertThat(store.findById(created.id()).orElseThrow().role()).isEqualTo(UserRole.ADMIN);
  }

  @Test
  void existingAdminRemainsAdminWithoutDowngrade() {
    InMemoryUserAccountStore store = new InMemoryUserAccountStore();
    VerifiedFirebaseIdentity identity =
        TestIdentities.emailPasswordIdentity("existing-admin", true);
    ProvisionedUserAccount created =
        service(store, allowlistedAdminPolicy("existing-admin")).bootstrap(identity);

    ProvisionedUserAccount bootstrapped = service(store).bootstrap(identity);

    assertThat(created.role()).isEqualTo("ADMIN");
    assertThat(bootstrapped.role()).isEqualTo("ADMIN");
    assertThat(bootstrapped.id()).isEqualTo(created.id());
  }

  @Test
  void nonAllowlistedExistingAccountIsUnchanged() {
    InMemoryUserAccountStore store = new InMemoryUserAccountStore();
    VerifiedFirebaseIdentity identity =
        TestIdentities.emailPasswordIdentity("not-allowlisted", true);
    ProvisionedUserAccount created = service(store).bootstrap(identity);

    ProvisionedUserAccount bootstrapped =
        service(store, allowlistedAdminPolicy("different-admin")).bootstrap(identity);

    assertThat(bootstrapped.id()).isEqualTo(created.id());
    assertThat(bootstrapped.role()).isEqualTo("VIEWER");
    assertThat(store.findById(created.id()).orElseThrow().role()).isEqualTo(UserRole.VIEWER);
  }

  @Test
  void disabledEventAdminBootstrapPolicyLeavesExistingAccountUnchanged() {
    InMemoryUserAccountStore store = new InMemoryUserAccountStore();
    VerifiedFirebaseIdentity identity =
        TestIdentities.emailPasswordIdentity("configured-admin-but-disabled", true);
    ProvisionedUserAccount created = service(store).bootstrap(identity);

    ProvisionedUserAccount bootstrapped =
        service(store, ignored -> UserRole.VIEWER).bootstrap(identity);

    assertThat(bootstrapped.id()).isEqualTo(created.id());
    assertThat(bootstrapped.role()).isEqualTo("VIEWER");
    assertThat(store.findById(created.id()).orElseThrow().role()).isEqualTo(UserRole.VIEWER);
  }

  @Test
  void suspendedAllowlistedAccountRemainsSuspendedAndBlocked() {
    InMemoryUserAccountStore store = new InMemoryUserAccountStore();
    VerifiedFirebaseIdentity identity =
        TestIdentities.emailPasswordIdentity("suspended-admin-candidate", true);
    ProvisionedUserAccount created = service(store).bootstrap(identity);
    UserAccount stored = store.findById(created.id()).orElseThrow();
    stored.suspend();

    assertThatThrownBy(
            () ->
                service(store, allowlistedAdminPolicy("suspended-admin-candidate"))
                    .bootstrap(identity))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.errorCode()).isEqualTo(ApiErrorCode.ACCOUNT_SUSPENDED));
    assertThat(stored.status()).isEqualTo(AccountStatus.SUSPENDED);
    assertThat(stored.role()).isEqualTo(UserRole.VIEWER);
  }

  @Test
  void repeatedAllowlistedBootstrapPromotesExistingAccountOnlyOnce(CapturedOutput output) {
    InMemoryUserAccountStore store = new InMemoryUserAccountStore();
    VerifiedFirebaseIdentity identity =
        TestIdentities.emailPasswordIdentity("repeat-admin-candidate", true);
    ProvisionedUserAccount created = service(store).bootstrap(identity);
    UserAccountBootstrapService adminBootstrapService =
        service(store, allowlistedAdminPolicy("repeat-admin-candidate"));

    ProvisionedUserAccount first = adminBootstrapService.bootstrap(identity);
    ProvisionedUserAccount second = adminBootstrapService.bootstrap(identity);

    assertThat(first.id()).isEqualTo(created.id());
    assertThat(second.id()).isEqualTo(created.id());
    assertThat(first.role()).isEqualTo("ADMIN");
    assertThat(second.role()).isEqualTo("ADMIN");
    assertThat(occurrences(output.toString(), "event=user_bootstrap_existing_promoted_to_admin"))
        .isEqualTo(1);
  }

  @Test
  void repeatedBootstrapReturnsSameUser() {
    InMemoryUserAccountStore store = new InMemoryUserAccountStore();
    UserAccountBootstrapService service = service(store);
    VerifiedFirebaseIdentity identity = TestIdentities.emailPasswordIdentity("repeat-user", true);

    ProvisionedUserAccount first = service.bootstrap(identity);
    ProvisionedUserAccount second = service.bootstrap(identity);

    assertThat(second.id()).isEqualTo(first.id());
    assertThat(store.countByFirebaseUid("repeat-user")).isEqualTo(1);
  }

  @Test
  void bootstrapLogsCreationAndExistingSynchronizationWithoutRawFirebaseValues(
      CapturedOutput output) {
    InMemoryUserAccountStore store = new InMemoryUserAccountStore();
    UserAccountBootstrapService service = service(store);
    VerifiedFirebaseIdentity identity =
        TestIdentities.emailPasswordIdentity("private-observable-bootstrap-user", true);

    service.bootstrap(identity);
    service.bootstrap(identity);

    assertThat(output)
        .contains("event=user_bootstrap_created")
        .contains("event=user_bootstrap_existing_synchronized")
        .contains("event=user_bootstrap_completed")
        .contains("firebaseUidFingerprint=sha256:")
        .doesNotContain("private-observable-bootstrap-user")
        .doesNotContain("private-observable-bootstrap-user@example.com")
        .doesNotContain("Firebase private-observable-bootstrap-user");
  }

  @Test
  void existingUserSyncsTrustedFieldsButDoesNotOverwriteCustomizedProfile() {
    InMemoryUserAccountStore store = new InMemoryUserAccountStore();
    UserAccountBootstrapService service = service(store);
    ProvisionedUserAccount created =
        service.bootstrap(TestIdentities.emailPasswordIdentity("sync-user", true));
    UserAccount stored = store.findById(created.id()).orElseThrow();
    stored.updateProfile("Local Name", "https://example.com/local.png", "Bio", Instant.now());

    ProvisionedUserAccount synced = service.bootstrap(TestIdentities.googleIdentity("sync-user"));

    assertThat(synced.displayName()).isEqualTo("Local Name");
    assertThat(synced.avatarUrl()).isEqualTo("https://example.com/local.png");
    assertThat(synced.role()).isEqualTo("VIEWER");
    assertThat(synced.status()).isEqualTo("ACTIVE");
  }

  @Test
  void suspendedAccountIsRejectedAndStatusIsNotChangedFromFirebaseData() {
    InMemoryUserAccountStore store = new InMemoryUserAccountStore();
    UserAccountBootstrapService service = service(store);
    ProvisionedUserAccount created =
        service.bootstrap(TestIdentities.emailPasswordIdentity("suspended-user", true));
    store.findById(created.id()).orElseThrow().suspend();

    assertThatThrownBy(() -> service.bootstrap(TestIdentities.googleIdentity("suspended-user")))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.errorCode()).isEqualTo(ApiErrorCode.ACCOUNT_SUSPENDED));
    assertThat(store.findById(created.id()).orElseThrow().status())
        .isEqualTo(AccountStatus.SUSPENDED);
  }

  @Test
  void disabledAccountIsRejected() {
    InMemoryUserAccountStore store = new InMemoryUserAccountStore();
    UserAccountBootstrapService service = service(store);
    ProvisionedUserAccount created =
        service.bootstrap(TestIdentities.emailPasswordIdentity("disabled-user", true));
    store.findById(created.id()).orElseThrow().disable();

    assertThatThrownBy(() -> service.bootstrap(TestIdentities.googleIdentity("disabled-user")))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.errorCode()).isEqualTo(ApiErrorCode.ACCOUNT_DISABLED));
  }

  private static UserAccountBootstrapService service(InMemoryUserAccountStore store) {
    return service(store, identity -> UserRole.VIEWER);
  }

  private static UserAccountBootstrapService service(
      InMemoryUserAccountStore store, InitialUserRolePolicy initialUserRolePolicy) {
    return new UserAccountBootstrapService(
        store,
        initialUserRolePolicy,
        new TestTransactionManager(),
        Clock.fixed(Instant.parse("2026-07-17T12:00:00Z"), ZoneOffset.UTC));
  }

  private static InitialUserRolePolicy allowlistedAdminPolicy(String... firebaseUids) {
    Set<String> allowlist = Set.of(firebaseUids);
    return identity ->
        allowlist.contains(identity.firebaseUid()) ? UserRole.ADMIN : UserRole.VIEWER;
  }

  private static int occurrences(String text, String value) {
    int count = 0;
    int index = text.indexOf(value);
    while (index >= 0) {
      count++;
      index = text.indexOf(value, index + value.length());
    }
    return count;
  }

  static final class InMemoryUserAccountStore implements UserAccountStore {
    private final Map<String, UserAccount> byFirebaseUid = new ConcurrentHashMap<>();
    private final Map<UUID, UserAccount> byId = new ConcurrentHashMap<>();

    @Override
    public Optional<UserAccount> findByFirebaseUid(String firebaseUid) {
      return Optional.ofNullable(byFirebaseUid.get(firebaseUid));
    }

    @Override
    public Optional<UserAccount> findById(UUID id) {
      return Optional.ofNullable(byId.get(id));
    }

    @Override
    public UserAccount saveAndFlush(UserAccount userAccount) {
      UserAccount existing = byFirebaseUid.putIfAbsent(userAccount.firebaseUid(), userAccount);
      if (existing != null && !existing.id().equals(userAccount.id())) {
        throw new DataIntegrityViolationException("duplicate firebase_uid");
      }
      byId.put(userAccount.id(), userAccount);
      return userAccount;
    }

    @Override
    public long countByFirebaseUid(String firebaseUid) {
      return byFirebaseUid.containsKey(firebaseUid) ? 1 : 0;
    }
  }

  static final class TestTransactionManager extends AbstractPlatformTransactionManager {
    @Override
    protected Object doGetTransaction() {
      return new Object();
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {}

    @Override
    protected void doCommit(DefaultTransactionStatus status) {}

    @Override
    protected void doRollback(DefaultTransactionStatus status) {}
  }
}
