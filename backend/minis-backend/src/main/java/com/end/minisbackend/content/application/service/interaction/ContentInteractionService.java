package com.end.minisbackend.content.application.service.interaction;

import com.end.minisbackend.content.application.port.ContentInteractionStore;
import com.end.minisbackend.identity.application.model.MinisPrincipal;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ContentInteractionService {
  private static final String SUSPENDED_STATUS = "SUSPENDED";
  private static final String DISABLED_STATUS = "DISABLED";

  private final ContentInteractionStore contentInteractionStore;
  private final PlatformTransactionManager transactionManager;
  private final Clock clock;

  public ContentInteractionService(
      ContentInteractionStore contentInteractionStore,
      PlatformTransactionManager transactionManager,
      Clock clock) {
    this.contentInteractionStore = contentInteractionStore;
    this.transactionManager = transactionManager;
    this.clock = clock;
  }

  public void like(MinisPrincipal principal, UUID contentId) {
    UUID userId = requireActiveProvisionedUser(principal);
    writeTransaction()
        .execute(
            status -> {
              int inserted =
                  contentInteractionStore.ensureLikedIfPublished(
                      userId, contentId, Instant.now(clock));
              if (inserted == 0 && !contentInteractionStore.isLiked(userId, contentId)) {
                rejectMissingPublishedContent(contentId);
              }
              return null;
            });
  }

  public void unlike(MinisPrincipal principal, UUID contentId) {
    UUID userId = requireActiveProvisionedUser(principal);
    writeTransaction()
        .execute(
            status -> {
              contentInteractionStore.removeLike(userId, contentId);
              return null;
            });
  }

  public void save(MinisPrincipal principal, UUID contentId) {
    UUID userId = requireActiveProvisionedUser(principal);
    writeTransaction()
        .execute(
            status -> {
              int inserted =
                  contentInteractionStore.ensureSavedIfPublished(
                      userId, contentId, Instant.now(clock));
              if (inserted == 0 && !contentInteractionStore.isSaved(userId, contentId)) {
                rejectMissingPublishedContent(contentId);
              }
              return null;
            });
  }

  public void unsave(MinisPrincipal principal, UUID contentId) {
    UUID userId = requireActiveProvisionedUser(principal);
    writeTransaction()
        .execute(
            status -> {
              contentInteractionStore.removeSave(userId, contentId);
              return null;
            });
  }

  private void rejectMissingPublishedContent(UUID contentId) {
    if (!contentInteractionStore.publishedContentExists(contentId)) {
      throw new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND);
    }
    throw new ApiException(ApiErrorCode.INTERNAL_ERROR);
  }

  private TransactionTemplate writeTransaction() {
    return new TransactionTemplate(transactionManager);
  }

  private static UUID requireActiveProvisionedUser(MinisPrincipal principal) {
    if (principal == null || !principal.hasProvisionedAccount()) {
      throw new ApiException(ApiErrorCode.ACCOUNT_NOT_PROVISIONED);
    }
    if (SUSPENDED_STATUS.equals(principal.accountStatus())) {
      throw new ApiException(ApiErrorCode.ACCOUNT_SUSPENDED);
    }
    if (DISABLED_STATUS.equals(principal.accountStatus())) {
      throw new ApiException(ApiErrorCode.ACCOUNT_DISABLED);
    }
    if (!principal.isActiveProvisionedAccount()) {
      throw new ApiException(ApiErrorCode.ACCESS_DENIED);
    }
    return principal.userId();
  }
}
