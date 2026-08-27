package com.end.minisbackend.creator.application;

import com.end.minisbackend.creator.domain.CreatorSlug;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CreatorProfileQueryService {
  private static final int MAX_PAGE_SIZE = 50;

  private final CreatorProfileStore creatorProfileStore;
  private final PlatformTransactionManager transactionManager;

  public CreatorProfileQueryService(
      CreatorProfileStore creatorProfileStore, PlatformTransactionManager transactionManager) {
    this.creatorProfileStore = creatorProfileStore;
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

  public CreatorProfileView publicCreator(String slug) {
    String normalizedSlug = normalizeSlug(slug);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);
    return transaction
        .execute(status -> creatorProfileStore.findPublicBySlug(normalizedSlug))
        .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND));
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
}
