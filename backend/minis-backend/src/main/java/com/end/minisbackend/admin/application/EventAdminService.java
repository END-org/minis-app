package com.end.minisbackend.admin.application;

import com.end.minisbackend.admin.domain.AdminAuditAction;
import com.end.minisbackend.admin.domain.AdminAuditEntry;
import com.end.minisbackend.creator.application.CreatorProfileAdministrationService;
import com.end.minisbackend.creator.application.CreatorProfileChange;
import com.end.minisbackend.creator.application.CreatorProfileView;
import com.end.minisbackend.creator.application.GrantCreatorProfileCommand;
import com.end.minisbackend.identity.application.MinisPrincipal;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import com.end.minisbackend.user.application.UserAccountStore;
import com.end.minisbackend.user.domain.AccountStatus;
import com.end.minisbackend.user.domain.UserAccount;
import com.end.minisbackend.user.domain.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class EventAdminService {
  private static final Logger LOGGER = LoggerFactory.getLogger(EventAdminService.class);
  private static final int MAX_PAGE_SIZE = 50;
  private static final String ADMIN_ROLE = "ADMIN";

  private final UserAccountStore userAccountStore;
  private final AdminUserQueryStore adminUserQueryStore;
  private final CreatorProfileAdministrationService creatorProfileAdministrationService;
  private final AdminAuditStore adminAuditStore;
  private final PlatformTransactionManager transactionManager;
  private final Clock clock;

  public EventAdminService(
      UserAccountStore userAccountStore,
      AdminUserQueryStore adminUserQueryStore,
      CreatorProfileAdministrationService creatorProfileAdministrationService,
      AdminAuditStore adminAuditStore,
      PlatformTransactionManager transactionManager,
      Clock clock) {
    this.userAccountStore = userAccountStore;
    this.adminUserQueryStore = adminUserQueryStore;
    this.creatorProfileAdministrationService = creatorProfileAdministrationService;
    this.adminAuditStore = adminAuditStore;
    this.transactionManager = transactionManager;
    this.clock = clock;
  }

  public AdminUserPage users(MinisPrincipal principal, String query, int page, int size) {
    requireAdmin(principal);
    int boundedPage = Math.max(page, 0);
    int boundedSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);
    return transaction.execute(
        status -> adminUserQueryStore.findUsers(query, boundedPage, boundedSize));
  }

  public AdminUserView user(MinisPrincipal principal, UUID userId) {
    requireAdmin(principal);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);
    return transaction
        .execute(status -> adminUserQueryStore.findUser(userId))
        .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND));
  }

  public AdminUserView grantCreator(
      MinisPrincipal principal,
      UUID targetUserId,
      GrantCreatorRoleCommand command,
      String correlationId) {
    AtomicReference<Boolean> changed = new AtomicReference<>(false);
    try {
      requireAdmin(principal);
      AdminUserView user =
          writeTransaction()
              .execute(
                  status -> {
                    Instant now = Instant.now(clock);
                    UserAccount target = requiredTarget(targetUserId);
                    if (target.role() == UserRole.ADMIN) {
                      throw new ApiException(ApiErrorCode.ACCESS_DENIED);
                    }
                    boolean roleChanged = target.role() != UserRole.CREATOR;
                    changed.set(roleChanged);
                    Optional<CreatorProfileView> existingProfile =
                        creatorProfileAdministrationService.profileByUserId(targetUserId);
                    if (roleChanged || existingProfile.isEmpty()) {
                      creatorProfileAdministrationService.ensureProfileForGrant(
                          targetUserId,
                          new GrantCreatorProfileCommand(
                              command.slug(),
                              command.displayName(),
                              command.bio(),
                              command.avatarUrl()),
                          now);
                    }
                    if (roleChanged) {
                      target.grantCreator(now);
                      userAccountStore.saveAndFlush(target);
                      audit(
                          principal.userId(),
                          targetUserId,
                          AdminAuditAction.CREATOR_GRANTED,
                          command.reason(),
                          correlationId,
                          now);
                    }
                    return reloadedUser(targetUserId);
                  });
      logCompletedAdminTransition(
          principal, targetUserId, AdminAuditAction.CREATOR_GRANTED, changed.get());
      return user;
    } catch (ApiException | IllegalStateException exception) {
      logFailedAdminTransition(
          principal, targetUserId, AdminAuditAction.CREATOR_GRANTED, exception);
      throw exception;
    }
  }

  public AdminUserView revokeCreator(
      MinisPrincipal principal,
      UUID targetUserId,
      AdminReasonCommand command,
      String correlationId) {
    AtomicReference<Boolean> changed = new AtomicReference<>(false);
    try {
      requireAdmin(principal);
      AdminUserView user =
          writeTransaction()
              .execute(
                  status -> {
                    Instant now = Instant.now(clock);
                    UserAccount target = requiredTarget(targetUserId);
                    if (target.role() == UserRole.ADMIN) {
                      throw new ApiException(ApiErrorCode.ACCESS_DENIED);
                    }
                    if (target.role() == UserRole.CREATOR) {
                      changed.set(true);
                      target.revokeCreator(now);
                      userAccountStore.saveAndFlush(target);
                      audit(
                          principal.userId(),
                          targetUserId,
                          AdminAuditAction.CREATOR_REVOKED,
                          command.reason(),
                          correlationId,
                          now);
                    }
                    return reloadedUser(targetUserId);
                  });
      logCompletedAdminTransition(
          principal, targetUserId, AdminAuditAction.CREATOR_REVOKED, changed.get());
      return user;
    } catch (ApiException | IllegalStateException exception) {
      logFailedAdminTransition(
          principal, targetUserId, AdminAuditAction.CREATOR_REVOKED, exception);
      throw exception;
    }
  }

  public AdminUserView suspend(
      MinisPrincipal principal,
      UUID targetUserId,
      AdminReasonCommand command,
      String correlationId) {
    AtomicReference<Boolean> changed = new AtomicReference<>(false);
    try {
      requireAdmin(principal);
      AdminUserView user =
          writeTransaction()
              .execute(
                  status -> {
                    Instant now = Instant.now(clock);
                    UserAccount target = requiredTarget(targetUserId);
                    if (target.id().equals(principal.userId())
                        || target.status() == AccountStatus.DISABLED) {
                      throw new ApiException(ApiErrorCode.ACCESS_DENIED);
                    }
                    if (target.status() == AccountStatus.ACTIVE) {
                      changed.set(true);
                      target.suspend(now);
                      userAccountStore.saveAndFlush(target);
                      audit(
                          principal.userId(),
                          targetUserId,
                          AdminAuditAction.ACCOUNT_SUSPENDED,
                          command.reason(),
                          correlationId,
                          now);
                    }
                    return reloadedUser(targetUserId);
                  });
      logCompletedAdminTransition(
          principal, targetUserId, AdminAuditAction.ACCOUNT_SUSPENDED, changed.get());
      return user;
    } catch (ApiException | IllegalStateException exception) {
      logFailedAdminTransition(
          principal, targetUserId, AdminAuditAction.ACCOUNT_SUSPENDED, exception);
      throw exception;
    }
  }

  public AdminUserView reactivate(
      MinisPrincipal principal,
      UUID targetUserId,
      AdminReasonCommand command,
      String correlationId) {
    AtomicReference<Boolean> changed = new AtomicReference<>(false);
    try {
      requireAdmin(principal);
      AdminUserView user =
          writeTransaction()
              .execute(
                  status -> {
                    Instant now = Instant.now(clock);
                    UserAccount target = requiredTarget(targetUserId);
                    if (target.status() == AccountStatus.DISABLED) {
                      throw new ApiException(ApiErrorCode.ACCESS_DENIED);
                    }
                    if (target.status() == AccountStatus.SUSPENDED) {
                      changed.set(true);
                      target.reactivate(now);
                      userAccountStore.saveAndFlush(target);
                      audit(
                          principal.userId(),
                          targetUserId,
                          AdminAuditAction.ACCOUNT_REACTIVATED,
                          command.reason(),
                          correlationId,
                          now);
                    }
                    return reloadedUser(targetUserId);
                  });
      logCompletedAdminTransition(
          principal, targetUserId, AdminAuditAction.ACCOUNT_REACTIVATED, changed.get());
      return user;
    } catch (ApiException | IllegalStateException exception) {
      logFailedAdminTransition(
          principal, targetUserId, AdminAuditAction.ACCOUNT_REACTIVATED, exception);
      throw exception;
    }
  }

  public AdminUserView verifyCreator(
      MinisPrincipal principal,
      UUID targetUserId,
      AdminReasonCommand command,
      String correlationId) {
    AtomicReference<Boolean> changed = new AtomicReference<>(false);
    try {
      requireAdmin(principal);
      AdminUserView user =
          writeTransaction()
              .execute(
                  status -> {
                    Instant now = Instant.now(clock);
                    CreatorProfileChange change =
                        creatorProfileAdministrationService.verify(targetUserId, now);
                    changed.set(change.changed());
                    if (change.changed()) {
                      audit(
                          principal.userId(),
                          targetUserId,
                          AdminAuditAction.CREATOR_VERIFIED,
                          command.reason(),
                          correlationId,
                          now);
                    }
                    return reloadedUser(targetUserId);
                  });
      logCompletedAdminTransition(
          principal, targetUserId, AdminAuditAction.CREATOR_VERIFIED, changed.get());
      return user;
    } catch (ApiException | IllegalStateException exception) {
      logFailedAdminTransition(
          principal, targetUserId, AdminAuditAction.CREATOR_VERIFIED, exception);
      throw exception;
    }
  }

  public AdminUserView unverifyCreator(
      MinisPrincipal principal,
      UUID targetUserId,
      AdminReasonCommand command,
      String correlationId) {
    AtomicReference<Boolean> changed = new AtomicReference<>(false);
    try {
      requireAdmin(principal);
      AdminUserView user =
          writeTransaction()
              .execute(
                  status -> {
                    Instant now = Instant.now(clock);
                    CreatorProfileChange change =
                        creatorProfileAdministrationService.unverify(targetUserId, now);
                    changed.set(change.changed());
                    if (change.changed()) {
                      audit(
                          principal.userId(),
                          targetUserId,
                          AdminAuditAction.CREATOR_UNVERIFIED,
                          command.reason(),
                          correlationId,
                          now);
                    }
                    return reloadedUser(targetUserId);
                  });
      logCompletedAdminTransition(
          principal, targetUserId, AdminAuditAction.CREATOR_UNVERIFIED, changed.get());
      return user;
    } catch (ApiException | IllegalStateException exception) {
      logFailedAdminTransition(
          principal, targetUserId, AdminAuditAction.CREATOR_UNVERIFIED, exception);
      throw exception;
    }
  }

  private UserAccount requiredTarget(UUID targetUserId) {
    return userAccountStore
        .findById(targetUserId)
        .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND));
  }

  private AdminUserView reloadedUser(UUID targetUserId) {
    return adminUserQueryStore
        .findUser(targetUserId)
        .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND));
  }

  private TransactionTemplate writeTransaction() {
    return new TransactionTemplate(transactionManager);
  }

  private void audit(
      UUID actorAdminId,
      UUID targetUserId,
      AdminAuditAction action,
      String reason,
      String correlationId,
      Instant now) {
    adminAuditStore.save(
        AdminAuditEntry.record(
            actorAdminId, targetUserId, action, normalize(reason), correlationId, now));
  }

  private static String normalize(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }

  private static void requireAdmin(MinisPrincipal principal) {
    if (principal == null
        || !principal.hasProvisionedAccount()
        || !ADMIN_ROLE.equals(principal.role())) {
      throw new ApiException(ApiErrorCode.ACCESS_DENIED);
    }
  }

  private static void logCompletedAdminTransition(
      MinisPrincipal principal, UUID targetUserId, AdminAuditAction action, boolean changed) {
    LOGGER.info(
        "event=admin_state_transition_completed correlationId={} actorLocalUserId={} "
            + "targetLocalUserId={} action={} changed={} result=success",
        correlationId(),
        actorId(principal),
        targetUserId,
        action.name(),
        changed);
  }

  private static void logFailedAdminTransition(
      MinisPrincipal principal,
      UUID targetUserId,
      AdminAuditAction action,
      RuntimeException exception) {
    LOGGER.warn(
        "event=admin_state_transition_failed correlationId={} actorLocalUserId={} "
            + "targetLocalUserId={} action={} result=failure apiErrorCode={} exceptionCategory={}",
        correlationId(),
        actorId(principal),
        targetUserId,
        action.name(),
        errorCode(exception),
        exception.getClass().getSimpleName());
  }

  private static String actorId(MinisPrincipal principal) {
    if (principal == null || !principal.hasProvisionedAccount()) {
      return "-";
    }
    return principal.userId().toString();
  }

  private static String errorCode(RuntimeException exception) {
    if (exception instanceof ApiException apiException) {
      return apiException.errorCode().name();
    }
    return ApiErrorCode.INTERNAL_ERROR.name();
  }

  private static String correlationId() {
    String correlationId = MDC.get("correlationId");
    return StringUtils.hasText(correlationId) ? correlationId : "-";
  }
}
