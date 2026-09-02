package com.end.minisbackend.admin.application.port;

import com.end.minisbackend.admin.application.model.AdminUserPage;
import com.end.minisbackend.admin.application.model.AdminUserView;
import java.util.Optional;
import java.util.UUID;

public interface AdminUserQueryStore {
  AdminUserPage findUsers(String query, int page, int size);

  Optional<AdminUserView> findUser(UUID userId);
}
