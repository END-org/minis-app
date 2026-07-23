package com.end.minisbackend.user.application;

import com.end.minisbackend.identity.application.MinisPrincipal;
import com.end.minisbackend.identity.application.ProvisionedUserAccount;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import com.end.minisbackend.user.domain.AccountStatus;
import com.end.minisbackend.user.domain.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Reads the current provisioned active MINIS user from the authenticated principal. */
@Service
public class CurrentUserService {
  private final UserAccountStore userAccountStore;
  private final PlatformTransactionManager transactionManager;

  public CurrentUserService(
      UserAccountStore userAccountStore, PlatformTransactionManager transactionManager) {
    this.userAccountStore = userAccountStore;
    this.transactionManager = transactionManager;
  }

  public ProvisionedUserAccount currentUser(MinisPrincipal principal) {
    if (principal == null || !principal.hasProvisionedAccount()) {
      throw new ApiException(ApiErrorCode.ACCOUNT_NOT_PROVISIONED);
    }
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);
    return transaction.execute(
        status -> {
          UserAccount account =
              userAccountStore
                  .findById(principal.userId())
                  .orElseThrow(() -> new ApiException(ApiErrorCode.ACCOUNT_NOT_PROVISIONED));
          rejectInactive(account);
          return UserAccountMapper.toProvisionedAccount(account);
        });
  }

  private static void rejectInactive(UserAccount account) {
    if (account.status() == AccountStatus.SUSPENDED) {
      throw new ApiException(ApiErrorCode.ACCOUNT_SUSPENDED);
    }
    if (account.status() == AccountStatus.DISABLED) {
      throw new ApiException(ApiErrorCode.ACCOUNT_DISABLED);
    }
  }
}
