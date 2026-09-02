package com.end.minisbackend.admin.domain.model;

import com.end.minisbackend.admin.domain.enums.AdminAuditAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.springframework.util.StringUtils;

@Entity
@Table(
    name = "event_admin_audit_entries",
    indexes = {
      @Index(name = "idx_event_admin_audit_target", columnList = "target_user_id, created_at"),
      @Index(name = "idx_event_admin_audit_actor", columnList = "actor_admin_id, created_at")
    })
public class AdminAuditEntry {
  private static final int REASON_MAX = 300;
  private static final int CORRELATION_ID_MAX = 128;

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(name = "actor_admin_id", nullable = false, updatable = false)
  private UUID actorAdminId;

  @Column(name = "target_user_id", nullable = false, updatable = false)
  private UUID targetUserId;

  @Enumerated(EnumType.STRING)
  @Column(name = "action_type", nullable = false, length = 64, updatable = false)
  private AdminAuditAction actionType;

  @Column(length = REASON_MAX, updatable = false)
  private String reason;

  @Column(name = "correlation_id", length = CORRELATION_ID_MAX, updatable = false)
  private String correlationId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected AdminAuditEntry() {}

  private AdminAuditEntry(
      UUID actorAdminId,
      UUID targetUserId,
      AdminAuditAction actionType,
      String reason,
      String correlationId,
      Instant createdAt) {
    this.id = UUID.randomUUID();
    this.actorAdminId = actorAdminId;
    this.targetUserId = targetUserId;
    this.actionType = actionType;
    this.reason = bounded(reason, REASON_MAX);
    this.correlationId = bounded(correlationId, CORRELATION_ID_MAX);
    this.createdAt = createdAt;
  }

  public static AdminAuditEntry record(
      UUID actorAdminId,
      UUID targetUserId,
      AdminAuditAction actionType,
      String reason,
      String correlationId,
      Instant createdAt) {
    return new AdminAuditEntry(
        actorAdminId, targetUserId, actionType, reason, correlationId, createdAt);
  }

  public UUID id() {
    return id;
  }

  public UUID actorAdminId() {
    return actorAdminId;
  }

  public UUID targetUserId() {
    return targetUserId;
  }

  public AdminAuditAction actionType() {
    return actionType;
  }

  public String reason() {
    return reason;
  }

  public String correlationId() {
    return correlationId;
  }

  public Instant createdAt() {
    return createdAt;
  }

  private static String bounded(String value, int maxLength) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.length() <= maxLength) {
      return trimmed;
    }
    return trimmed.substring(0, maxLength);
  }
}
