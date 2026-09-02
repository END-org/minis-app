package com.end.minisbackend.admin.application.service;

import com.end.minisbackend.admin.application.model.AdminReasonCommand;
import com.end.minisbackend.admin.application.model.AdminUserView;
import com.end.minisbackend.admin.application.port.AdminUserQueryStore;
import com.end.minisbackend.admin.domain.enums.AdminAuditAction;
import com.end.minisbackend.identity.application.model.MinisPrincipal;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import com.end.minisbackend.user.application.port.UserAccountStore;
import com.end.minisbackend.user.domain.enums.AccountStatus;
import com.end.minisbackend.user.domain.model.UserAccount;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AccountStatusAdministrationService {
  private final UserAccountStore userAccountStore;
  private final AdminUserQueryStore adminUserQueryStore;
  private final AdminOperationAudit adminOperationAudit;
  private final PlatformTransactionManager transactionManager;
  private final Clock clock;

  public AccountStatusAdministrationService(
      UserAccountStore userAccountStore,
      AdminUserQueryStore adminUserQueryStore,
      AdminOperationAudit adminOperationAudit,
      PlatformTransactionManager transactionManager,
      Clock clock) {
    this.userAccountStore = userAccountStore;
    this.adminUserQueryStore = adminUserQueryStore;
    this.adminOperationAudit = adminOperationAudit;
    this.transactionManager = transactionManager;
    this.clock = clock;
  }

  public AdminUserView suspend(
      MinisPrincipal principal,
      UUID targetUserId,
      AdminReasonCommand command,
      String correlationId) {
    AtomicReference<Boolean> changed = new AtomicReference<>(false);
    try {
      AdminAccessPolicy.requireAdmin(principal);
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
                      adminOperationAudit.record(
                          principal.userId(),
                          targetUserId,
                          AdminAuditAction.ACCOUNT_SUSPENDED,
                          command.reason(),
                          correlationId,
                          now);
                    }
                    return reloadedUser(targetUserId);
                  });
      adminOperationAudit.completed(
          principal, targetUserId, AdminAuditAction.ACCOUNT_SUSPENDED, changed.get());
      return user;
    } catch (ApiException | IllegalStateException exception) {
      adminOperationAudit.failed(
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
      AdminAccessPolicy.requireAdmin(principal);
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
                      adminOperationAudit.record(
                          principal.userId(),
                          targetUserId,
                          AdminAuditAction.ACCOUNT_REACTIVATED,
                          command.reason(),
                          correlationId,
                          now);
                    }
                    return reloadedUser(targetUserId);
                  });
      adminOperationAudit.completed(
          principal, targetUserId, AdminAuditAction.ACCOUNT_REACTIVATED, changed.get());
      return user;
    } catch (ApiException | IllegalStateException exception) {
      adminOperationAudit.failed(
          principal, targetUserId, AdminAuditAction.ACCOUNT_REACTIVATED, exception);
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
}
