package com.end.minisbackend.creator.application;

import com.end.minisbackend.creator.domain.CreatorSlug;
import com.end.minisbackend.identity.application.MinisPrincipal;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CreatorFollowService {
  private static final String SUSPENDED_STATUS = "SUSPENDED";
  private static final String DISABLED_STATUS = "DISABLED";

  private final CreatorProfileStore creatorProfileStore;
  private final CreatorFollowStore creatorFollowStore;
  private final PlatformTransactionManager transactionManager;
  private final Clock clock;

  public CreatorFollowService(
      CreatorProfileStore creatorProfileStore,
      CreatorFollowStore creatorFollowStore,
      PlatformTransactionManager transactionManager,
      Clock clock) {
    this.creatorProfileStore = creatorProfileStore;
    this.creatorFollowStore = creatorFollowStore;
    this.transactionManager = transactionManager;
    this.clock = clock;
  }

  public void follow(MinisPrincipal principal, String slug) {
    UUID followerUserId = requireActiveProvisionedUser(principal);
    String normalizedSlug = normalizeSlug(slug);
    writeTransaction()
        .execute(
            status -> {
              CreatorProfileView creator =
                  creatorProfileStore
                      .findPublicBySlug(normalizedSlug)
                      .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND));
              if (followerUserId.equals(creator.userId())) {
                throw new ApiException(ApiErrorCode.RESOURCE_CONFLICT);
              }
              int inserted =
                  creatorFollowStore.ensureFollowIfCreatorIsPublic(
                      followerUserId, creator.userId(), Instant.now(clock));
              if (inserted == 0
                  && !creatorFollowStore.isFollowing(followerUserId, creator.userId())) {
                throw new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND);
              }
              return null;
            });
  }

  public void unfollow(MinisPrincipal principal, String slug) {
    UUID followerUserId = requireActiveProvisionedUser(principal);
    String normalizedSlug = normalizeSlug(slug);
    writeTransaction()
        .execute(
            status -> {
              creatorFollowStore
                  .findCreatorUserIdBySlug(normalizedSlug)
                  .ifPresent(
                      creatorUserId ->
                          creatorFollowStore.removeFollow(followerUserId, creatorUserId));
              return null;
            });
  }

  private TransactionTemplate writeTransaction() {
    return new TransactionTemplate(transactionManager);
  }

  private static String normalizeSlug(String slug) {
    try {
      return CreatorSlug.normalize(slug);
    } catch (IllegalArgumentException exception) {
      throw new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, exception);
    }
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
