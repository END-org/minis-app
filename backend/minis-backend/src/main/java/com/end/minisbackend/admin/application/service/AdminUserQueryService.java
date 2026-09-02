package com.end.minisbackend.admin.application.service;

import com.end.minisbackend.admin.application.model.AdminUserPage;
import com.end.minisbackend.admin.application.model.AdminUserView;
import com.end.minisbackend.admin.application.port.AdminUserQueryStore;
import com.end.minisbackend.identity.application.model.MinisPrincipal;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AdminUserQueryService {
  private static final int MAX_PAGE_SIZE = 50;

  private final AdminUserQueryStore adminUserQueryStore;
  private final PlatformTransactionManager transactionManager;

  public AdminUserQueryService(
      AdminUserQueryStore adminUserQueryStore, PlatformTransactionManager transactionManager) {
    this.adminUserQueryStore = adminUserQueryStore;
    this.transactionManager = transactionManager;
  }

  public AdminUserPage users(MinisPrincipal principal, String query, int page, int size) {
    AdminAccessPolicy.requireAdmin(principal);
    int boundedPage = Math.max(page, 0);
    int boundedSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
    return readOnlyTransaction()
        .execute(status -> adminUserQueryStore.findUsers(query, boundedPage, boundedSize));
  }

  public AdminUserView user(MinisPrincipal principal, UUID userId) {
    AdminAccessPolicy.requireAdmin(principal);
    return readOnlyTransaction()
        .execute(status -> adminUserQueryStore.findUser(userId))
        .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND));
  }

  private TransactionTemplate readOnlyTransaction() {
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);
    return transaction;
  }
}
