package com.end.minisbackend.content.application.service.feed;

import com.end.minisbackend.content.application.model.feed.ContentCursor;
import com.end.minisbackend.content.application.model.feed.ContentPage;
import com.end.minisbackend.content.application.model.feed.PersonalContentView;
import com.end.minisbackend.content.application.model.feed.PublicContentItem;
import com.end.minisbackend.content.application.model.feed.PublishedContentView;
import com.end.minisbackend.content.application.model.interaction.ContentInteractionCursor;
import com.end.minisbackend.content.application.port.ContentInteractionStore;
import com.end.minisbackend.content.application.port.ContentQueryStore;
import com.end.minisbackend.content.domain.enums.ContentCategory;
import com.end.minisbackend.creator.application.model.CreatorProfileView;
import com.end.minisbackend.creator.application.service.read.CreatorProfileQueryService;
import com.end.minisbackend.identity.application.model.MinisPrincipal;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PublicContentQueryService {
  private static final int DEFAULT_LIMIT = 10;
  private static final int MAX_LIMIT = 30;
  private static final String SUSPENDED_STATUS = "SUSPENDED";
  private static final String DISABLED_STATUS = "DISABLED";

  private final ContentQueryStore contentQueryStore;
  private final ContentInteractionStore contentInteractionStore;
  private final CreatorProfileQueryService creatorProfileQueryService;
  private final PublicContentAssembler publicContentAssembler;
  private final PlatformTransactionManager transactionManager;

  public PublicContentQueryService(
      ContentQueryStore contentQueryStore,
      ContentInteractionStore contentInteractionStore,
      CreatorProfileQueryService creatorProfileQueryService,
      PublicContentAssembler publicContentAssembler,
      PlatformTransactionManager transactionManager) {
    this.contentQueryStore = contentQueryStore;
    this.contentInteractionStore = contentInteractionStore;
    this.creatorProfileQueryService = creatorProfileQueryService;
    this.publicContentAssembler = publicContentAssembler;
    this.transactionManager = transactionManager;
  }

  public ContentPage feed(String cursor, Integer limit, String category) {
    return feed(null, cursor, limit, category);
  }

  public ContentPage feed(MinisPrincipal principal, String cursor, Integer limit, String category) {
    ContentCategory parsedCategory =
        category == null || category.isBlank() ? null : ContentCategory.parse(category);
    ContentCursor decodedCursor = ContentCursor.decode(cursor);
    int boundedLimit = boundedLimit(limit);
    List<PublishedContentView> rows =
        readOnlyTransaction()
            .execute(
                status ->
                    contentQueryStore.findFeed(parsedCategory, decodedCursor, boundedLimit + 1));
    return toPage(rows, boundedLimit, viewerUserIdOrNull(principal));
  }

  public PublicContentItem content(UUID contentId) {
    return content(null, contentId);
  }

  public PublicContentItem content(MinisPrincipal principal, UUID contentId) {
    PublishedContentView row =
        readOnlyTransaction()
            .execute(status -> contentQueryStore.findPublishedById(contentId))
            .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND));
    return publicContentAssembler.assemble(List.of(row), viewerUserIdOrNull(principal)).get(0);
  }

  public ContentPage creatorContent(String slug, String cursor, Integer limit) {
    return creatorContent(null, slug, cursor, limit);
  }

  public ContentPage creatorContent(
      MinisPrincipal principal, String slug, String cursor, Integer limit) {
    CreatorProfileView creator = creatorProfileQueryService.publicCreator(slug);
    ContentCursor decodedCursor = ContentCursor.decode(cursor);
    int boundedLimit = boundedLimit(limit);
    List<PublishedContentView> rows =
        readOnlyTransaction()
            .execute(
                status ->
                    contentQueryStore.findCreatorContent(
                        creator.userId(), decodedCursor, boundedLimit + 1));
    return toPage(rows, boundedLimit, viewerUserIdOrNull(principal));
  }

  public ContentPage likedContent(MinisPrincipal principal, String cursor, Integer size) {
    UUID viewerUserId = requireActiveProvisionedUser(principal);
    ContentInteractionCursor decodedCursor = ContentInteractionCursor.decode(cursor);
    int boundedLimit = boundedLimit(size);
    List<PersonalContentView> rows =
        readOnlyTransaction()
            .execute(
                status ->
                    contentInteractionStore.findLikedContent(
                        viewerUserId, decodedCursor, boundedLimit + 1));
    return toPersonalPage(rows, boundedLimit, viewerUserId);
  }

  public ContentPage savedContent(MinisPrincipal principal, String cursor, Integer size) {
    UUID viewerUserId = requireActiveProvisionedUser(principal);
    ContentInteractionCursor decodedCursor = ContentInteractionCursor.decode(cursor);
    int boundedLimit = boundedLimit(size);
    List<PersonalContentView> rows =
        readOnlyTransaction()
            .execute(
                status ->
                    contentInteractionStore.findSavedContent(
                        viewerUserId, decodedCursor, boundedLimit + 1));
    return toPersonalPage(rows, boundedLimit, viewerUserId);
  }

  private ContentPage toPage(List<PublishedContentView> rows, int limit, UUID viewerUserId) {
    boolean hasNext = rows.size() > limit;
    List<PublishedContentView> visibleRows = hasNext ? rows.subList(0, limit) : rows;
    String nextCursor = hasNext ? cursorFor(visibleRows.get(visibleRows.size() - 1)) : null;
    return new ContentPage(
        publicContentAssembler.assemble(visibleRows, viewerUserId), nextCursor, limit);
  }

  private ContentPage toPersonalPage(List<PersonalContentView> rows, int limit, UUID viewerUserId) {
    boolean hasNext = rows.size() > limit;
    List<PersonalContentView> visibleRows = hasNext ? rows.subList(0, limit) : rows;
    String nextCursor =
        hasNext ? interactionCursorFor(visibleRows.get(visibleRows.size() - 1)) : null;
    return new ContentPage(
        publicContentAssembler.assemble(
            visibleRows.stream().map(PersonalContentView::content).toList(), viewerUserId),
        nextCursor,
        limit);
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

  private static String interactionCursorFor(PersonalContentView row) {
    return new ContentInteractionCursor(row.relationshipCreatedAt(), row.content().id()).encode();
  }

  private static UUID viewerUserIdOrNull(MinisPrincipal principal) {
    return principal != null && principal.isActiveProvisionedAccount() ? principal.userId() : null;
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
