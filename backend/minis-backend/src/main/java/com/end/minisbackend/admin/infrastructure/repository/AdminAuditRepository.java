package com.end.minisbackend.admin.infrastructure.repository;

import com.end.minisbackend.admin.domain.model.AdminAuditEntry;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AdminAuditRepository extends JpaRepository<AdminAuditEntry, UUID> {}
