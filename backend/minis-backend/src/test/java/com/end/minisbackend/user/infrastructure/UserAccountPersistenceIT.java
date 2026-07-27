package com.end.minisbackend.user.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.end.minisbackend.TestIdentities;
import com.end.minisbackend.identity.application.ProvisionedUserAccount;
import com.end.minisbackend.identity.application.UserAccountProvisioningGateway;
import com.end.minisbackend.user.application.UserAccountStore;
import com.end.minisbackend.user.domain.UserAccount;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class UserAccountPersistenceIT {
  @Autowired private UserAccountStore userAccountStore;
  @Autowired private UserAccountProvisioningGateway userAccountProvisioningGateway;

  @Test
  void firebaseUidHasUniqueDatabaseConstraint() {
    UserAccount first =
        UserAccount.createViewer("unique-db-user", "first@example.com", true, null, null, now());
    UserAccount duplicate =
        UserAccount.createViewer("unique-db-user", "second@example.com", true, null, null, now());

    userAccountStore.saveAndFlush(first);

    assertThatThrownBy(() -> userAccountStore.saveAndFlush(duplicate))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void persistsUuidEnumsAndTimestamps() {
    UserAccount created =
        userAccountStore.saveAndFlush(
            UserAccount.createViewer(
                "persistence-user", "persist@example.com", true, "Persist User", null, now()));

    UserAccount loaded = userAccountStore.findById(created.id()).orElseThrow();

    assertThat(loaded.id()).isInstanceOf(UUID.class);
    assertThat(loaded.role().name()).isEqualTo("VIEWER");
    assertThat(loaded.status().name()).isEqualTo("ACTIVE");
    assertThat(loaded.createdAt()).isEqualTo(Instant.parse("2026-07-17T12:00:00Z"));
  }

  @Test
  void verifiedFederatedBootstrapPersistsViewerAccount() {
    ProvisionedUserAccount account =
        userAccountProvisioningGateway.bootstrap(TestIdentities.googleIdentity("google-db-user"));

    assertThat(account.role()).isEqualTo("VIEWER");
    assertThat(account.status()).isEqualTo("ACTIVE");
    assertThat(account.emailVerified()).isTrue();
    assertThat(userAccountStore.countByFirebaseUid("google-db-user")).isEqualTo(1);
  }

  @Test
  void unverifiedPasswordBootstrapDoesNotPersistUserRow() {
    assertThatThrownBy(
            () ->
                userAccountProvisioningGateway.bootstrap(
                    TestIdentities.emailPasswordIdentity("unverified-db-user", false)))
        .hasMessageContaining("Email address must be verified before bootstrap");

    assertThat(userAccountStore.countByFirebaseUid("unverified-db-user")).isZero();
  }

  @Test
  void concurrentBootstrapCreatesOneUserRow() throws Exception {
    int workerCount = 6;
    ExecutorService executorService = Executors.newFixedThreadPool(workerCount);
    CyclicBarrier barrier = new CyclicBarrier(workerCount);
    List<Callable<ProvisionedUserAccount>> tasks = new ArrayList<>();
    for (int index = 0; index < workerCount; index++) {
      tasks.add(
          () -> {
            barrier.await();
            return userAccountProvisioningGateway.bootstrap(
                TestIdentities.emailPasswordIdentity("concurrent-user", true));
          });
    }

    try {
      List<Future<ProvisionedUserAccount>> futures = executorService.invokeAll(tasks);
      Set<UUID> ids =
          futures.stream()
              .map(UserAccountPersistenceIT::getUnchecked)
              .map(ProvisionedUserAccount::id)
              .collect(Collectors.toSet());

      assertThat(ids).hasSize(1);
      assertThat(userAccountStore.countByFirebaseUid("concurrent-user")).isEqualTo(1);
    } finally {
      executorService.shutdownNow();
    }
  }

  private static ProvisionedUserAccount getUnchecked(Future<ProvisionedUserAccount> future) {
    try {
      return future.get();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Interrupted while waiting for bootstrap result", exception);
    } catch (java.util.concurrent.ExecutionException exception) {
      throw new AssertionError("Bootstrap worker failed", exception);
    }
  }

  private static Instant now() {
    return Instant.parse("2026-07-17T12:00:00Z");
  }
}
