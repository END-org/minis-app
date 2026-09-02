package com.end.minisbackend.admin.infrastructure.repository;

import com.end.minisbackend.admin.application.port.AdminAuditStore;
import com.end.minisbackend.admin.domain.model.AdminAuditEntry;
import org.springframework.stereotype.Repository;

@Repository
class AdminAuditPersistenceAdapter implements AdminAuditStore {
  private final AdminAuditRepository repository;

  AdminAuditPersistenceAdapter(AdminAuditRepository repository) {
    this.repository = repository;
  }

  @Override
  public AdminAuditEntry save(AdminAuditEntry entry) {
    return repository.save(entry);
  }
}
