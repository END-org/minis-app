package com.end.minisbackend.admin.infrastructure;

import com.end.minisbackend.admin.domain.AdminAuditEntry;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AdminAuditRepository extends JpaRepository<AdminAuditEntry, UUID> {}
