package com.end.minisbackend.user.application;

import com.end.minisbackend.identity.application.LocalUserIdentity;
import com.end.minisbackend.identity.application.LocalUserIdentityGateway;
import com.end.minisbackend.identity.application.ProvisionedUserAccount;
import com.end.minisbackend.identity.application.UserAccountProvisioningGateway;
import com.end.minisbackend.identity.domain.VerifiedFirebaseIdentity;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import com.end.minisbackend.user.domain.AccountStatus;
import com.end.minisbackend.user.domain.UserAccount;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
class UserAccountBootstrapService
    implements UserAccountProvisioningGateway, LocalUserIdentityGateway {
  private final UserAccountStore userAccountStore;
  private final PlatformTransactionManager transactionManager;
  private final Clock clock;

  UserAccountBootstrapService(
      UserAccountStore userAccountStore, PlatformTransactionManager transactionManager) {
    this.userAccountStore = userAccountStore;
    this.transactionManager = transactionManager;
    this.clock = Clock.systemUTC();
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
    try {
      return writeTransaction().execute(status -> bootstrapInsideTransaction(identity));
    } catch (DataIntegrityViolationException exception) {
      if (!isFirebaseUidConflict(exception)) {
        throw exception;
      }
      return writeTransaction().execute(status -> recoverConcurrentBootstrap(identity));
    }
  }

  private ProvisionedUserAccount bootstrapInsideTransaction(VerifiedFirebaseIdentity identity) {
    Optional<UserAccount> existing = userAccountStore.findByFirebaseUid(identity.firebaseUid());
    if (existing.isPresent()) {
      return synchronizeExisting(existing.get(), identity);
    }

    Instant now = Instant.now(clock);
    UserAccount created =
        UserAccount.createViewer(
            identity.firebaseUid(),
            identity.email(),
            identity.emailVerified(),
            identity.displayName(),
            identity.pictureUrl(),
            now);
    return UserAccountMapper.toProvisionedAccount(userAccountStore.saveAndFlush(created));
  }

  private ProvisionedUserAccount recoverConcurrentBootstrap(VerifiedFirebaseIdentity identity) {
    UserAccount account =
        userAccountStore
            .findByFirebaseUid(identity.firebaseUid())
            .orElseThrow(() -> new ApiException(ApiErrorCode.INTERNAL_ERROR));
    return synchronizeExisting(account, identity);
  }

  private ProvisionedUserAccount synchronizeExisting(
      UserAccount account, VerifiedFirebaseIdentity identity) {
    rejectInactive(account);
    account.synchronizeTrustedIdentity(
        identity.email(),
        identity.emailVerified(),
        identity.displayName(),
        identity.pictureUrl(),
        Instant.now(clock));
    return UserAccountMapper.toProvisionedAccount(userAccountStore.saveAndFlush(account));
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

  private static boolean isFirebaseUidConflict(DataIntegrityViolationException exception) {
    String message = exception.getMostSpecificCause().getMessage();
    return message != null && message.toLowerCase(Locale.ROOT).contains("firebase_uid");
  }
}
