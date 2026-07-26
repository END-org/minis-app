package com.end.minisbackend.admin.application;

import com.end.minisbackend.admin.domain.AdminAuditEntry;

public interface AdminAuditStore {
  AdminAuditEntry save(AdminAuditEntry entry);
}
