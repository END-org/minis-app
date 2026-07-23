package com.end.minisbackend.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.end.minisbackend.TestIdentities;
import com.end.minisbackend.identity.application.ProvisionedUserAccount;
import com.end.minisbackend.identity.domain.VerifiedFirebaseIdentity;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import com.end.minisbackend.user.domain.AccountStatus;
import com.end.minisbackend.user.domain.UserAccount;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

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
    return new UserAccountBootstrapService(store, new TestTransactionManager());
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
