package com.end.minisbackend.user.application;

import com.end.minisbackend.identity.application.LocalUserIdentity;
import com.end.minisbackend.identity.application.LocalUserIdentityGateway;
import com.end.minisbackend.identity.application.ProvisionedUserAccount;
import com.end.minisbackend.identity.application.UserAccountProvisioningGateway;
import com.end.minisbackend.identity.domain.VerifiedFirebaseIdentity;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import com.end.minisbackend.shared.observability.SafeIdentityFingerprint;
import com.end.minisbackend.user.domain.AccountStatus;
import com.end.minisbackend.user.domain.UserAccount;
import com.end.minisbackend.user.domain.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
class UserAccountBootstrapService
    implements UserAccountProvisioningGateway, LocalUserIdentityGateway {
  private static final Logger LOGGER = LoggerFactory.getLogger(UserAccountBootstrapService.class);
  private static final String CREATED_OUTCOME = "created";
  private static final String EXISTING_SYNCHRONIZED_OUTCOME = "existing_synchronized";
  private static final String EXISTING_PROMOTED_TO_ADMIN_OUTCOME = "existing_promoted_to_admin";
  private static final String CONCURRENT_CONFLICT_RECOVERED_OUTCOME =
      "concurrent_conflict_recovered";

  private final UserAccountStore userAccountStore;
  private final InitialUserRolePolicy initialUserRolePolicy;
  private final PlatformTransactionManager transactionManager;
  private final Clock clock;

  UserAccountBootstrapService(
      UserAccountStore userAccountStore,
      InitialUserRolePolicy initialUserRolePolicy,
      PlatformTransactionManager transactionManager,
      Clock clock) {
    this.userAccountStore = userAccountStore;
    this.initialUserRolePolicy = initialUserRolePolicy;
    this.transactionManager = transactionManager;
    this.clock = clock;
  }

  @Override
  public Optional<LocalUserIdentity> findByFirebaseUid(String firebaseUid) {
    TransactionTemplate transaction = readOnlyTransaction();
    return transaction
        .execute(status -> userAccountStore.findByFirebaseUid(firebaseUid))
        .map(UserAccountMapper::toLocalIdentity);
  }

  @Override
  public ProvisionedUserAccount bootstrap(VerifiedFirebaseIdentity identity) {
    long startedAt = System.nanoTime();
    String firebaseUidFingerprint = SafeIdentityFingerprint.firebaseUid(identity.firebaseUid());
    LOGGER.info(
        "event=user_bootstrap_started correlationId={} firebaseUidFingerprint={} provider={}",
        correlationId(),
        firebaseUidFingerprint,
        valueOrEmpty(identity.signInProvider()));
    try {
      rejectUnverifiedPasswordIdentity(identity);
      AtomicReference<String> outcome = new AtomicReference<>();
      ProvisionedUserAccount account =
          writeTransaction().execute(status -> bootstrapInsideTransaction(identity, outcome));
      logSuccessfulBootstrap(account, identity, outcome.get(), startedAt, firebaseUidFingerprint);
      return account;
    } catch (DataIntegrityViolationException exception) {
      if (!isFirebaseUidConflict(exception)) {
        throw exception;
      }
      ProvisionedUserAccount account =
          writeTransaction().execute(status -> recoverConcurrentBootstrap(identity));
      logSuccessfulBootstrap(
          account,
          identity,
          CONCURRENT_CONFLICT_RECOVERED_OUTCOME,
          startedAt,
          firebaseUidFingerprint);
      return account;
    } catch (ApiException exception) {
      LOGGER.warn(
          "event=user_bootstrap_rejected correlationId={} firebaseUidFingerprint={} provider={} "
              + "apiErrorCode={} durationMs={}",
          correlationId(),
          firebaseUidFingerprint,
          valueOrEmpty(identity.signInProvider()),
          exception.errorCode().name(),
          elapsedMillis(startedAt));
      throw exception;
    }
  }

  private ProvisionedUserAccount bootstrapInsideTransaction(
      VerifiedFirebaseIdentity identity, AtomicReference<String> outcome) {
    Optional<UserAccount> existing = userAccountStore.findByFirebaseUid(identity.firebaseUid());
    if (existing.isPresent()) {
      return synchronizeExisting(existing.get(), identity, outcome);
    }

    Instant now = Instant.now(clock);
    UserAccount created =
        UserAccount.create(
            identity.firebaseUid(),
            identity.email(),
            identity.emailVerified(),
            identity.displayName(),
            identity.pictureUrl(),
            initialUserRolePolicy.roleFor(identity),
            now);
    outcome.set(CREATED_OUTCOME);
    return UserAccountMapper.toProvisionedAccount(userAccountStore.saveAndFlush(created));
  }

  private ProvisionedUserAccount recoverConcurrentBootstrap(VerifiedFirebaseIdentity identity) {
    UserAccount account =
        userAccountStore
            .findByFirebaseUid(identity.firebaseUid())
            .orElseThrow(() -> new ApiException(ApiErrorCode.INTERNAL_ERROR));
    return synchronizeExisting(account, identity, new AtomicReference<>());
  }

  private ProvisionedUserAccount synchronizeExisting(
      UserAccount account, VerifiedFirebaseIdentity identity, AtomicReference<String> outcome) {
    rejectInactive(account);
    Instant now = Instant.now(clock);
    boolean promotedToAdmin = promoteExistingAllowlistedAdmin(account, identity, now);
    account.synchronizeTrustedIdentity(
        identity.email(),
        identity.emailVerified(),
        identity.displayName(),
        identity.pictureUrl(),
        now);
    outcome.set(
        promotedToAdmin ? EXISTING_PROMOTED_TO_ADMIN_OUTCOME : EXISTING_SYNCHRONIZED_OUTCOME);
    return UserAccountMapper.toProvisionedAccount(userAccountStore.saveAndFlush(account));
  }

  private boolean promoteExistingAllowlistedAdmin(
      UserAccount account, VerifiedFirebaseIdentity identity, Instant now) {
    if (initialUserRolePolicy.roleFor(identity) != UserRole.ADMIN) {
      return false;
    }
    return account.promoteToAdmin(now);
  }

  private TransactionTemplate readOnlyTransaction() {
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);
    return transaction;
  }

  private TransactionTemplate writeTransaction() {
    return new TransactionTemplate(transactionManager);
  }

  private static void rejectInactive(UserAccount account) {
    if (account.status() == AccountStatus.SUSPENDED) {
      throw new ApiException(ApiErrorCode.ACCOUNT_SUSPENDED);
    }
    if (account.status() == AccountStatus.DISABLED) {
      throw new ApiException(ApiErrorCode.ACCOUNT_DISABLED);
    }
  }

  private static void rejectUnverifiedPasswordIdentity(VerifiedFirebaseIdentity identity) {
    if (identity.isEmailPasswordSignIn() && !identity.emailVerified()) {
      throw new ApiException(ApiErrorCode.EMAIL_NOT_VERIFIED);
    }
  }

  private static boolean isFirebaseUidConflict(DataIntegrityViolationException exception) {
    String message = exception.getMostSpecificCause().getMessage();
    return message != null && message.toLowerCase(Locale.ROOT).contains("firebase_uid");
  }

  private static void logSuccessfulBootstrap(
      ProvisionedUserAccount account,
      VerifiedFirebaseIdentity identity,
      String outcome,
      long startedAt,
      String firebaseUidFingerprint) {
    LOGGER.info(
        "event=user_bootstrap_{} correlationId={} firebaseUidFingerprint={} localUserId={} "
            + "role={} status={} provider={} durationMs={}",
        outcome,
        correlationId(),
        firebaseUidFingerprint,
        account.id(),
        account.role(),
        account.status(),
        valueOrEmpty(identity.signInProvider()),
        elapsedMillis(startedAt));
    LOGGER.info(
        "event=user_bootstrap_completed correlationId={} firebaseUidFingerprint={} "
            + "localUserId={} created={} role={} status={} provider={} durationMs={}",
        correlationId(),
        firebaseUidFingerprint,
        account.id(),
        CREATED_OUTCOME.equals(outcome),
        account.role(),
        account.status(),
        valueOrEmpty(identity.signInProvider()),
        elapsedMillis(startedAt));
  }

  private static long elapsedMillis(long startedAt) {
    return Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
  }

  private static String correlationId() {
    return valueOrEmpty(MDC.get("correlationId"));
  }

  private static String valueOrEmpty(String value) {
    return value == null || value.isBlank() ? "-" : value;
  }
}
