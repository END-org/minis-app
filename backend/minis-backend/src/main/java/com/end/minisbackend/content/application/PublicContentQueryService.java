package com.end.minisbackend.content.application;

import com.end.minisbackend.content.domain.ContentCategory;
import com.end.minisbackend.creator.application.CreatorProfileQueryService;
import com.end.minisbackend.creator.application.CreatorProfileView;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PublicContentQueryService {
  private static final Logger LOGGER = LoggerFactory.getLogger(PublicContentQueryService.class);
  private static final int DEFAULT_LIMIT = 10;
  private static final int MAX_LIMIT = 30;

  private final ContentQueryStore contentQueryStore;
  private final ContentStorage contentStorage;
  private final CreatorProfileQueryService creatorProfileQueryService;
  private final PlatformTransactionManager transactionManager;

  public PublicContentQueryService(
      ContentQueryStore contentQueryStore,
      ContentStorage contentStorage,
      CreatorProfileQueryService creatorProfileQueryService,
      PlatformTransactionManager transactionManager) {
    this.contentQueryStore = contentQueryStore;
    this.contentStorage = contentStorage;
    this.creatorProfileQueryService = creatorProfileQueryService;
    this.transactionManager = transactionManager;
  }

  public ContentPage feed(String cursor, Integer limit, String category) {
    ContentCategory parsedCategory =
        category == null || category.isBlank() ? null : ContentCategory.parse(category);
    ContentCursor decodedCursor = ContentCursor.decode(cursor);
    int boundedLimit = boundedLimit(limit);
    List<PublishedContentView> rows =
        readOnlyTransaction()
            .execute(
                status ->
                    contentQueryStore.findFeed(parsedCategory, decodedCursor, boundedLimit + 1));
    return toPage(rows, boundedLimit);
  }

  public PublicContentItem content(UUID contentId) {
    PublishedContentView row =
        readOnlyTransaction()
            .execute(status -> contentQueryStore.findPublishedById(contentId))
            .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND));
    return toPublicItem(row);
  }

  public ContentPage creatorContent(String slug, String cursor, Integer limit) {
    CreatorProfileView creator = creatorProfileQueryService.publicCreator(slug);
    ContentCursor decodedCursor = ContentCursor.decode(cursor);
    int boundedLimit = boundedLimit(limit);
    List<PublishedContentView> rows =
        readOnlyTransaction()
            .execute(
                status ->
                    contentQueryStore.findCreatorContent(
                        creator.userId(), decodedCursor, boundedLimit + 1));
    return toPage(rows, boundedLimit);
  }

  private ContentPage toPage(List<PublishedContentView> rows, int limit) {
    boolean hasNext = rows.size() > limit;
    List<PublishedContentView> visibleRows = hasNext ? rows.subList(0, limit) : rows;
    String nextCursor = hasNext ? cursorFor(visibleRows.get(visibleRows.size() - 1)) : null;
    return new ContentPage(
        visibleRows.stream().map(this::toPublicItem).toList(), nextCursor, limit);
  }

  private PublicContentItem toPublicItem(PublishedContentView row) {
    SignedReadUrl signedReadUrl = signedUrl(row);
    return new PublicContentItem(
        row.id(),
        new CreatorAttribution(
            row.creatorUserId(),
            row.creatorSlug(),
            row.creatorDisplayName(),
            row.creatorAvatarUrl(),
            row.creatorVerified()),
        row.title(),
        row.description(),
        row.category().name(),
        row.category().displayName(),
        new SignedMediaReference(
            signedReadUrl.url().toString(),
            signedReadUrl.expiresAt(),
            row.mediaContentType(),
            row.mediaSizeBytes(),
            row.durationMs()),
        row.publishedAt());
  }

  private SignedReadUrl signedUrl(PublishedContentView row) {
    try {
      return contentStorage.signedReadUrl(row.mediaObjectKey());
    } catch (ContentStorageException exception) {
      LOGGER.warn(
          "event=signed_url_generation_failed contentId={} storageError={}",
          row.id(),
          exception.getClass().getSimpleName());
      throw new ApiException(ApiErrorCode.INTERNAL_ERROR, exception);
    }
  }

  private TransactionTemplate readOnlyTransaction() {
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);
    return transaction;
  }

  private static int boundedLimit(Integer limit) {
    if (limit == null) {
      return DEFAULT_LIMIT;
    }
    return Math.max(1, Math.min(limit, MAX_LIMIT));
  }

  private static String cursorFor(PublishedContentView row) {
    return new ContentCursor(row.publishedAt(), row.id()).encode();
  }
}
