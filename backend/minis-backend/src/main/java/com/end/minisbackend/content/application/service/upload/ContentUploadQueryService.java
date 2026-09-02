package com.end.minisbackend.content.application.service.upload;

import com.end.minisbackend.content.application.model.feed.PublicContentItem;
import com.end.minisbackend.content.application.model.upload.ContentUploadCursor;
import com.end.minisbackend.content.application.model.upload.ContentUploadItem;
import com.end.minisbackend.content.application.model.upload.ContentUploadPage;
import com.end.minisbackend.content.application.model.upload.ContentUploadView;
import com.end.minisbackend.content.application.port.ContentUploadStore;
import com.end.minisbackend.content.application.service.feed.PublicContentQueryService;
import com.end.minisbackend.identity.application.model.MinisPrincipal;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ContentUploadQueryService {
  private static final int DEFAULT_SIZE = 10;
  private static final int MAX_SIZE = 30;
  private static final String CREATOR_ROLE = "CREATOR";
  private static final String SUSPENDED_STATUS = "SUSPENDED";
  private static final String DISABLED_STATUS = "DISABLED";

  private final ContentUploadStore contentUploadStore;
  private final PublicContentQueryService publicContentQueryService;
  private final PlatformTransactionManager transactionManager;

  public ContentUploadQueryService(
      ContentUploadStore contentUploadStore,
      PublicContentQueryService publicContentQueryService,
      PlatformTransactionManager transactionManager) {
    this.contentUploadStore = contentUploadStore;
    this.publicContentQueryService = publicContentQueryService;
    this.transactionManager = transactionManager;
  }

  public ContentUploadPage uploads(MinisPrincipal principal, String cursor, Integer size) {
    UUID creatorUserId = requireActiveCreator(principal);
    ContentUploadCursor decodedCursor = ContentUploadCursor.decode(cursor);
    int boundedSize = boundedSize(size);
    List<ContentUploadView> rows =
        readOnlyTransaction()
            .execute(
                status ->
                    contentUploadStore.findUploadsByCreatorUserId(
                        creatorUserId, decodedCursor, boundedSize + 1));
    return toPage(principal, rows, boundedSize);
  }

  ContentUploadItem uploadForOwner(MinisPrincipal principal, UUID contentId) {
    UUID creatorUserId = requireActiveCreator(principal);
    ContentUploadView row =
        readOnlyTransaction()
            .execute(
                status ->
                    contentUploadStore.findUploadByCreatorUserIdAndId(creatorUserId, contentId))
            .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND));
    return toItem(principal, row);
  }

  private ContentUploadPage toPage(
      MinisPrincipal principal, List<ContentUploadView> rows, int boundedSize) {
    boolean hasNext = rows.size() > boundedSize;
    List<ContentUploadView> visibleRows = hasNext ? rows.subList(0, boundedSize) : rows;
    String nextCursor = hasNext ? cursorFor(visibleRows.get(visibleRows.size() - 1)) : null;
    return new ContentUploadPage(
        visibleRows.stream().map(row -> toItem(principal, row)).toList(), nextCursor, boundedSize);
  }

  private ContentUploadItem toItem(MinisPrincipal principal, ContentUploadView row) {
    PublicContentItem publicContent =
        row.publishedAt() == null || !"PUBLISHED".equals(row.status().name())
            ? null
            : publicContentQueryService.content(principal, row.id());
    return new ContentUploadItem(
        row.id(),
        row.title(),
        row.description(),
        row.category(),
        row.categoryLabel(),
        row.status().name(),
        row.failureCode(),
        row.failureReason(),
        row.createdAt(),
        row.uploadStartedAt(),
        row.uploadCompletedAt(),
        row.publishedAt(),
        row.mediaContentType(),
        row.mediaSizeBytes(),
        row.durationMs(),
        publicContent);
  }

  private TransactionTemplate readOnlyTransaction() {
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);
    return transaction;
  }

  private static int boundedSize(Integer size) {
    if (size == null) {
      return DEFAULT_SIZE;
    }
    return Math.max(1, Math.min(size, MAX_SIZE));
  }

  private static String cursorFor(ContentUploadView row) {
    return new ContentUploadCursor(row.createdAt(), row.id()).encode();
  }

  static UUID requireActiveCreator(MinisPrincipal principal) {
    if (principal == null || !principal.hasProvisionedAccount()) {
      throw new ApiException(ApiErrorCode.ACCOUNT_NOT_PROVISIONED);
    }
    if (SUSPENDED_STATUS.equals(principal.accountStatus())) {
      throw new ApiException(ApiErrorCode.ACCOUNT_SUSPENDED);
    }
    if (DISABLED_STATUS.equals(principal.accountStatus())) {
      throw new ApiException(ApiErrorCode.ACCOUNT_DISABLED);
    }
    if (!principal.isActiveProvisionedAccount() || !CREATOR_ROLE.equals(principal.role())) {
      throw new ApiException(ApiErrorCode.ACCESS_DENIED);
    }
    return principal.userId();
  }
}
