package com.end.minisbackend.user.application;

import com.end.minisbackend.identity.application.MinisPrincipal;
import com.end.minisbackend.identity.application.ProvisionedUserAccount;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import com.end.minisbackend.user.domain.AccountStatus;
import com.end.minisbackend.user.domain.UserAccount;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Reads the current provisioned active MINIS user from the authenticated principal. */
@Service
public class CurrentUserService {
  private static final Logger LOGGER = LoggerFactory.getLogger(CurrentUserService.class);

  private final UserAccountStore userAccountStore;
  private final PlatformTransactionManager transactionManager;
  private final Clock clock;

  public CurrentUserService(
      UserAccountStore userAccountStore,
      PlatformTransactionManager transactionManager,
      Clock clock) {
    this.userAccountStore = userAccountStore;
    this.transactionManager = transactionManager;
    this.clock = clock;
  }

  public ProvisionedUserAccount currentUser(MinisPrincipal principal) {
    if (principal == null || !principal.hasProvisionedAccount()) {
      throw new ApiException(ApiErrorCode.ACCOUNT_NOT_PROVISIONED);
    }
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);
    return transaction.execute(
        status -> {
          UserAccount account =
              userAccountStore
                  .findById(principal.userId())
                  .orElseThrow(() -> new ApiException(ApiErrorCode.ACCOUNT_NOT_PROVISIONED));
          rejectInactive(account);
          return UserAccountMapper.toProvisionedAccount(account);
        });
  }

  public ProvisionedUserAccount updateProfile(
      MinisPrincipal principal, UpdateCurrentProfileCommand command) {
    if (principal == null || !principal.hasProvisionedAccount()) {
      throw new ApiException(ApiErrorCode.ACCOUNT_NOT_PROVISIONED);
    }
    long startedAt = System.nanoTime();
    AtomicReference<List<String>> changedFields = new AtomicReference<>(List.of());
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    ProvisionedUserAccount account =
        transaction.execute(
            status -> {
              UserAccount userAccount =
                  userAccountStore
                      .findById(principal.userId())
                      .orElseThrow(() -> new ApiException(ApiErrorCode.ACCOUNT_NOT_PROVISIONED));
              rejectInactive(userAccount);
              String existingDisplayName = userAccount.displayName();
              String existingAvatarUrl = userAccount.avatarUrl();
              String existingBio = userAccount.bio();
              try {
                userAccount.updateProfile(
                    command.displayName(), command.avatarUrl(), command.bio(), Instant.now(clock));
              } catch (IllegalArgumentException exception) {
                throw new ApiException(ApiErrorCode.VALIDATION_ERROR, exception);
              }
              changedFields.set(
                  changedFields(existingDisplayName, existingAvatarUrl, existingBio, userAccount));
              return UserAccountMapper.toProvisionedAccount(
                  userAccountStore.saveAndFlush(userAccount));
            });
    LOGGER.info(
        "event=user_profile_updated correlationId={} localUserId={} changedFields={} durationMs={}",
        valueOrEmpty(MDC.get("correlationId")),
        account.id(),
        changedFields.get(),
        elapsedMillis(startedAt));
    return account;
  }

  private static void rejectInactive(UserAccount account) {
    if (account.status() == AccountStatus.SUSPENDED) {
      throw new ApiException(ApiErrorCode.ACCOUNT_SUSPENDED);
    }
    if (account.status() == AccountStatus.DISABLED) {
      throw new ApiException(ApiErrorCode.ACCOUNT_DISABLED);
    }
  }

  private static List<String> changedFields(
      String existingDisplayName,
      String existingAvatarUrl,
      String existingBio,
      UserAccount account) {
    List<String> fields = new ArrayList<>();
    if (!Objects.equals(existingDisplayName, account.displayName())) {
      fields.add("displayName");
    }
    if (!Objects.equals(existingAvatarUrl, account.avatarUrl())) {
      fields.add("avatarUrl");
    }
    if (!Objects.equals(existingBio, account.bio())) {
      fields.add("bio");
    }
    return List.copyOf(fields);
  }

  private static long elapsedMillis(long startedAt) {
    return Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
  }

  private static String valueOrEmpty(String value) {
    return value == null || value.isBlank() ? "-" : value;
  }
}
