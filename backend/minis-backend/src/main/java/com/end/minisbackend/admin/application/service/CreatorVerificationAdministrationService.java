package com.end.minisbackend.admin.application.service;

import com.end.minisbackend.admin.application.model.AdminReasonCommand;
import com.end.minisbackend.admin.application.model.AdminUserView;
import com.end.minisbackend.admin.application.port.AdminUserQueryStore;
import com.end.minisbackend.admin.domain.enums.AdminAuditAction;
import com.end.minisbackend.creator.application.model.CreatorProfileChange;
import com.end.minisbackend.creator.application.service.profile.CreatorProfileAdministrationService;
import com.end.minisbackend.identity.application.model.MinisPrincipal;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CreatorVerificationAdministrationService {
  private final AdminUserQueryStore adminUserQueryStore;
  private final CreatorProfileAdministrationService creatorProfileAdministrationService;
  private final AdminOperationAudit adminOperationAudit;
  private final PlatformTransactionManager transactionManager;
  private final Clock clock;

  public CreatorVerificationAdministrationService(
      AdminUserQueryStore adminUserQueryStore,
      CreatorProfileAdministrationService creatorProfileAdministrationService,
      AdminOperationAudit adminOperationAudit,
      PlatformTransactionManager transactionManager,
      Clock clock) {
    this.adminUserQueryStore = adminUserQueryStore;
    this.creatorProfileAdministrationService = creatorProfileAdministrationService;
    this.adminOperationAudit = adminOperationAudit;
    this.transactionManager = transactionManager;
    this.clock = clock;
  }

  public AdminUserView verifyCreator(
      MinisPrincipal principal,
      UUID targetUserId,
      AdminReasonCommand command,
      String correlationId) {
    return changeVerification(
        principal,
        targetUserId,
        command,
        correlationId,
        AdminAuditAction.CREATOR_VERIFIED,
        creatorProfileAdministrationService::verify);
  }

  public AdminUserView unverifyCreator(
      MinisPrincipal principal,
      UUID targetUserId,
      AdminReasonCommand command,
      String correlationId) {
    return changeVerification(
        principal,
        targetUserId,
        command,
        correlationId,
        AdminAuditAction.CREATOR_UNVERIFIED,
        creatorProfileAdministrationService::unverify);
  }

  private AdminUserView changeVerification(
      MinisPrincipal principal,
      UUID targetUserId,
      AdminReasonCommand command,
      String correlationId,
      AdminAuditAction action,
      BiFunction<UUID, Instant, CreatorProfileChange> transition) {
    AtomicReference<Boolean> changed = new AtomicReference<>(false);
    try {
      AdminAccessPolicy.requireAdmin(principal);
      AdminUserView user =
          writeTransaction()
              .execute(
                  status -> {
                    Instant now = Instant.now(clock);
                    CreatorProfileChange change = transition.apply(targetUserId, now);
                    changed.set(change.changed());
                    if (change.changed()) {
                      adminOperationAudit.record(
                          principal.userId(),
                          targetUserId,
                          action,
                          command.reason(),
                          correlationId,
                          now);
                    }
                    return reloadedUser(targetUserId);
                  });
      adminOperationAudit.completed(principal, targetUserId, action, changed.get());
      return user;
    } catch (ApiException | IllegalStateException exception) {
      adminOperationAudit.failed(principal, targetUserId, action, exception);
      throw exception;
    }
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
