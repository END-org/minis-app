package com.end.minisbackend.user.infrastructure.repository;

import com.end.minisbackend.user.application.port.UserAccountStore;
import com.end.minisbackend.user.domain.model.UserAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class UserAccountPersistenceAdapter implements UserAccountStore {
  private final UserAccountRepository repository;

  UserAccountPersistenceAdapter(UserAccountRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<UserAccount> findByFirebaseUid(String firebaseUid) {
    return repository.findByFirebaseUid(firebaseUid);
  }

  @Override
  public Optional<UserAccount> findById(UUID id) {
    return repository.findById(id);
  }

  @Override
  public UserAccount saveAndFlush(UserAccount userAccount) {
    return repository.saveAndFlush(userAccount);
  }

  @Override
  public long countByFirebaseUid(String firebaseUid) {
    return repository.countByFirebaseUid(firebaseUid);
  }
}
