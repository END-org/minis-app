package com.end.minisbackend.admin.application.service;

import com.end.minisbackend.admin.application.port.AdminAuditStore;
import com.end.minisbackend.admin.domain.enums.AdminAuditAction;
import com.end.minisbackend.admin.domain.model.AdminAuditEntry;
import com.end.minisbackend.identity.application.model.MinisPrincipal;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class AdminOperationAudit {
  private static final Logger LOGGER = LoggerFactory.getLogger(AdminOperationAudit.class);

  private final AdminAuditStore adminAuditStore;

  AdminOperationAudit(AdminAuditStore adminAuditStore) {
    this.adminAuditStore = adminAuditStore;
  }

  void record(
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

  void completed(
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

  void failed(
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

  private static String normalize(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
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
