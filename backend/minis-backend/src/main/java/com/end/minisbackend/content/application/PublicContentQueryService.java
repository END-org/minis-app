package com.end.minisbackend.content.application;

import com.end.minisbackend.content.domain.ContentCategory;
import com.end.minisbackend.creator.application.CreatorFollowStore;
import com.end.minisbackend.creator.application.CreatorProfileQueryService;
import com.end.minisbackend.creator.application.CreatorProfileView;
import com.end.minisbackend.identity.application.MinisPrincipal;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
  private static final String SUSPENDED_STATUS = "SUSPENDED";
  private static final String DISABLED_STATUS = "DISABLED";

  private final ContentQueryStore contentQueryStore;
  private final ContentInteractionStore contentInteractionStore;
  private final ContentStorage contentStorage;
  private final CreatorProfileQueryService creatorProfileQueryService;
  private final CreatorFollowStore creatorFollowStore;
  private final PlatformTransactionManager transactionManager;

  public PublicContentQueryService(
      ContentQueryStore contentQueryStore,
      ContentInteractionStore contentInteractionStore,
      ContentStorage contentStorage,
      CreatorProfileQueryService creatorProfileQueryService,
      CreatorFollowStore creatorFollowStore,
      PlatformTransactionManager transactionManager) {
    this.contentQueryStore = contentQueryStore;
    this.contentInteractionStore = contentInteractionStore;
    this.contentStorage = contentStorage;
    this.creatorProfileQueryService = creatorProfileQueryService;
    this.creatorFollowStore = creatorFollowStore;
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
    return toPublicItems(List.of(row), viewerUserIdOrNull(principal)).get(0);
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
    return new ContentPage(toPublicItems(visibleRows, viewerUserId), nextCursor, limit);
  }

  private ContentPage toPersonalPage(List<PersonalContentView> rows, int limit, UUID viewerUserId) {
    boolean hasNext = rows.size() > limit;
    List<PersonalContentView> visibleRows = hasNext ? rows.subList(0, limit) : rows;
    String nextCursor =
        hasNext ? interactionCursorFor(visibleRows.get(visibleRows.size() - 1)) : null;
    return new ContentPage(
        toPublicItems(
            visibleRows.stream().map(PersonalContentView::content).toList(), viewerUserId),
        nextCursor,
        limit);
  }

  private List<PublicContentItem> toPublicItems(
      List<PublishedContentView> rows, UUID viewerUserId) {
    Map<UUID, ContentSocialState> states = statesByContentId(rows, viewerUserId);
    return rows.stream().map(row -> toPublicItem(row, states.get(row.id()))).toList();
  }

  private PublicContentItem toPublicItem(PublishedContentView row, ContentSocialState state) {
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
        row.publishedAt(),
        state.likeCount(),
        state.liked(),
        state.saved(),
        state.following());
  }

  private Map<UUID, ContentSocialState> statesByContentId(
      List<PublishedContentView> rows, UUID viewerUserId) {
    if (rows.isEmpty()) {
      return Map.of();
    }
    List<UUID> contentIds = rows.stream().map(PublishedContentView::id).toList();
    List<UUID> creatorUserIds =
        rows.stream().map(PublishedContentView::creatorUserId).distinct().toList();
    Map<UUID, Long> likeCounts = contentInteractionStore.likeCountsByContentIds(contentIds);
    Set<UUID> likedContentIds =
        viewerUserId == null
            ? Set.of()
            : contentInteractionStore.likedContentIds(viewerUserId, contentIds);
    Set<UUID> savedContentIds =
        viewerUserId == null
            ? Set.of()
            : contentInteractionStore.savedContentIds(viewerUserId, contentIds);
    Set<UUID> followedCreatorIds =
        viewerUserId == null
            ? Set.of()
            : creatorFollowStore.followedCreatorIds(viewerUserId, creatorUserIds);
    Map<UUID, ContentSocialState> states = new HashMap<>();
    for (PublishedContentView row : rows) {
      states.put(
          row.id(),
          new ContentSocialState(
              likeCounts.getOrDefault(row.id(), 0L),
              likedContentIds.contains(row.id()),
              savedContentIds.contains(row.id()),
              followedCreatorIds.contains(row.creatorUserId())));
    }
    return Map.copyOf(states);
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
