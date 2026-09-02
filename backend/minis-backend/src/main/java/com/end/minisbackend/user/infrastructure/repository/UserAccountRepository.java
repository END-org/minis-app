package com.end.minisbackend.user.infrastructure.repository;

import com.end.minisbackend.user.domain.model.UserAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
  Optional<UserAccount> findByFirebaseUid(String firebaseUid);

  long countByFirebaseUid(String firebaseUid);
}
