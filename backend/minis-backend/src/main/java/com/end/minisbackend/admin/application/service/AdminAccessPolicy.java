package com.end.minisbackend.admin.application.service;

import com.end.minisbackend.identity.application.model.MinisPrincipal;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;

final class AdminAccessPolicy {
  private static final String ADMIN_ROLE = "ADMIN";

  private AdminAccessPolicy() {}

  static void requireAdmin(MinisPrincipal principal) {
    if (principal == null
        || !principal.hasProvisionedAccount()
        || !ADMIN_ROLE.equals(principal.role())) {
      throw new ApiException(ApiErrorCode.ACCESS_DENIED);
    }
  }
}
