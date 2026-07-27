package com.end.minisbackend.user.application;

import com.end.minisbackend.identity.domain.VerifiedFirebaseIdentity;
import com.end.minisbackend.user.domain.UserRole;

public interface InitialUserRolePolicy {
  UserRole roleFor(VerifiedFirebaseIdentity identity);
}
