package com.end.minisbackend.user.application.port;

import com.end.minisbackend.identity.domain.model.VerifiedFirebaseIdentity;
import com.end.minisbackend.user.domain.enums.UserRole;

public interface InitialUserRolePolicy {
  UserRole roleFor(VerifiedFirebaseIdentity identity);
}
