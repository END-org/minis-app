package com.end.minisbackend.admin.application.service;

import com.end.minisbackend.admin.application.model.AdminReasonCommand;
import com.end.minisbackend.admin.application.model.AdminUserView;
import com.end.minisbackend.admin.application.model.GrantCreatorRoleCommand;
import com.end.minisbackend.admin.application.port.AdminUserQueryStore;
import com.end.minisbackend.admin.domain.enums.AdminAuditAction;
import com.end.minisbackend.creator.application.model.CreatorProfileView;
import com.end.minisbackend.creator.application.model.GrantCreatorProfileCommand;
import com.end.minisbackend.creator.application.service.profile.CreatorProfileAdministrationService;
import com.end.minisbackend.identity.application.model.MinisPrincipal;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import com.end.minisbackend.user.application.port.UserAccountStore;
import com.end.minisbackend.user.domain.enums.UserRole;
import com.end.minisbackend.user.domain.model.UserAccount;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CreatorRoleAdministrationService {
  private final UserAccountStore userAccountStore;
  private final AdminUserQueryStore adminUserQueryStore;
  private final CreatorProfileAdministrationService creatorProfileAdministrationService;
  private final AdminOperationAudit adminOperationAudit;
  private final PlatformTransactionManager transactionManager;
  private final Clock clock;

  public CreatorRoleAdministrationService(
      UserAccountStore userAccountStore,
      AdminUserQueryStore adminUserQueryStore,
      CreatorProfileAdministrationService creatorProfileAdministrationService,
      AdminOperationAudit adminOperationAudit,
      PlatformTransactionManager transactionManager,
      Clock clock) {
    this.userAccountStore = userAccountStore;
    this.adminUserQueryStore = adminUserQueryStore;
    this.creatorProfileAdministrationService = creatorProfileAdministrationService;
    this.adminOperationAudit = adminOperationAudit;
    this.transactionManager = transactionManager;
    this.clock = clock;
  }

  public AdminUserView grantCreator(
      MinisPrincipal principal,
      UUID targetUserId,
      GrantCreatorRoleCommand command,
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
                      adminOperationAudit.record(
                          principal.userId(),
                          targetUserId,
                          AdminAuditAction.CREATOR_GRANTED,
                          command.reason(),
                          correlationId,
                          now);
                    }
                    return reloadedUser(targetUserId);
                  });
      adminOperationAudit.completed(
          principal, targetUserId, AdminAuditAction.CREATOR_GRANTED, changed.get());
      return user;
    } catch (ApiException | IllegalStateException exception) {
      adminOperationAudit.failed(
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
      AdminAccessPolicy.requireAdmin(principal);
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
                      adminOperationAudit.record(
                          principal.userId(),
                          targetUserId,
                          AdminAuditAction.CREATOR_REVOKED,
                          command.reason(),
                          correlationId,
                          now);
                    }
                    return reloadedUser(targetUserId);
                  });
      adminOperationAudit.completed(
          principal, targetUserId, AdminAuditAction.CREATOR_REVOKED, changed.get());
      return user;
    } catch (ApiException | IllegalStateException exception) {
      adminOperationAudit.failed(
          principal, targetUserId, AdminAuditAction.CREATOR_REVOKED, exception);
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
