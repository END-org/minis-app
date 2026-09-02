package com.end.minisbackend.creator.application;

import com.end.minisbackend.creator.domain.CreatorSlug;
import com.end.minisbackend.identity.application.MinisPrincipal;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CreatorProfileQueryService {
  private static final int MAX_PAGE_SIZE = 50;

  private final CreatorProfileStore creatorProfileStore;
  private final CreatorFollowStore creatorFollowStore;
  private final PlatformTransactionManager transactionManager;

  public CreatorProfileQueryService(
      CreatorProfileStore creatorProfileStore,
      CreatorFollowStore creatorFollowStore,
      PlatformTransactionManager transactionManager) {
    this.creatorProfileStore = creatorProfileStore;
    this.creatorFollowStore = creatorFollowStore;
    this.transactionManager = transactionManager;
  }

  public CreatorDirectoryPage publicCreators(int page, int size) {
    int boundedPage = Math.max(page, 0);
    int boundedSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);
    return transaction.execute(
        status -> creatorProfileStore.findPublicCreators(boundedPage, boundedSize));
  }

  public CreatorDirectoryView publicCreators(MinisPrincipal principal, int page, int size) {
    CreatorDirectoryPage profiles = publicCreators(page, size);
    Set<UUID> followedCreatorIds =
        followedCreatorIds(
            principal, profiles.creators().stream().map(CreatorProfileView::userId).toList());
    List<CreatorProfileItem> items =
        profiles.creators().stream()
            .map(
                profile ->
                    new CreatorProfileItem(profile, followedCreatorIds.contains(profile.userId())))
            .toList();
    return new CreatorDirectoryView(items, profiles.page(), profiles.size(), profiles.hasNext());
  }

  public CreatorProfileView publicCreator(String slug) {
    String normalizedSlug = normalizeSlug(slug);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);
    return transaction
        .execute(status -> creatorProfileStore.findPublicBySlug(normalizedSlug))
        .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND));
  }

  public CreatorProfileItem publicCreator(MinisPrincipal principal, String slug) {
    CreatorProfileView profile = publicCreator(slug);
    boolean following = isFollowing(principal, profile.userId());
    return new CreatorProfileItem(profile, following);
  }

  public CreatorProfileView publicCreatorByUserId(UUID userId) {
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);
    return transaction
        .execute(status -> creatorProfileStore.findPublicByUserId(userId))
        .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND));
  }

  private static String normalizeSlug(String slug) {
    try {
      return CreatorSlug.normalize(slug);
    } catch (IllegalArgumentException exception) {
      throw new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, exception);
    }
  }

  private Set<UUID> followedCreatorIds(MinisPrincipal principal, List<UUID> creatorUserIds) {
    if (!canReadViewerState(principal) || creatorUserIds.isEmpty()) {
      return Set.of();
    }
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);
    return transaction.execute(
        status -> creatorFollowStore.followedCreatorIds(principal.userId(), creatorUserIds));
  }

  private boolean isFollowing(MinisPrincipal principal, UUID creatorUserId) {
    if (!canReadViewerState(principal)) {
      return false;
    }
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);
    return Boolean.TRUE.equals(
        transaction.execute(
            status -> creatorFollowStore.isFollowing(principal.userId(), creatorUserId)));
  }

  private static boolean canReadViewerState(MinisPrincipal principal) {
    return principal != null && principal.isActiveProvisionedAccount();
  }
}
