package com.end.minisbackend.admin.application.port;

import com.end.minisbackend.admin.domain.model.AdminAuditEntry;

public interface AdminAuditStore {
  AdminAuditEntry save(AdminAuditEntry entry);
}
