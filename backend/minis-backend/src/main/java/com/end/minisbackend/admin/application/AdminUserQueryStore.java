package com.end.minisbackend.admin.application;

import java.util.Optional;
import java.util.UUID;

public interface AdminUserQueryStore {
  AdminUserPage findUsers(String query, int page, int size);

  Optional<AdminUserView> findUser(UUID userId);
}
