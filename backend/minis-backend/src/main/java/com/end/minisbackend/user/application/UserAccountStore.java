package com.end.minisbackend.user.application;

import com.end.minisbackend.user.domain.UserAccount;
import java.util.Optional;
import java.util.UUID;

/** Persistence boundary owned by the user application service. */
public interface UserAccountStore {
  Optional<UserAccount> findByFirebaseUid(String firebaseUid);

  Optional<UserAccount> findById(UUID id);

  UserAccount saveAndFlush(UserAccount userAccount);

  long countByFirebaseUid(String firebaseUid);
}
