package com.end.minisbackend.content.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.end.minisbackend.TestIdentities;
import com.end.minisbackend.content.application.ContentInteractionService;
import com.end.minisbackend.content.application.ContentPage;
import com.end.minisbackend.content.application.ContentStorage;
import com.end.minisbackend.content.application.PublicContentItem;
import com.end.minisbackend.content.application.PublicContentQueryService;
import com.end.minisbackend.content.application.SignedReadUrl;
import com.end.minisbackend.identity.application.MinisPrincipal;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class ContentInteractionPersistenceIT {
  private static final Instant BASE_TIME = Instant.parse("2026-07-18T12:00:00Z");

  @Autowired private ContentInteractionService contentInteractionService;
  @Autowired private PublicContentQueryService publicContentQueryService;
  @Autowired private JdbcTemplate jdbcTemplate;

  @MockitoBean private ContentStorage contentStorage;

  @BeforeEach
  void setUp() {
    when(contentStorage.signedReadUrl(anyString()))
        .thenReturn(
            new SignedReadUrl(
                URI.create("http://localhost:9000/minis-event-media/content/imports/test.mp4"),
                Instant.parse("2026-07-18T12:10:00Z")));
  }

  @Test
  void likePersistenceIsIdempotentAndDerivedCountUpdates() {
    UUID viewerId = insertUser("like-viewer", "VIEWER", "ACTIVE");
    UUID creatorId = insertCreator("like-creator");
    UUID contentId = insertContent(creatorId, "PUBLISHED", BASE_TIME);
    MinisPrincipal viewer = principal(viewerId, "VIEWER", "ACTIVE");

    contentInteractionService.like(viewer, contentId);
    contentInteractionService.like(viewer, contentId);

    assertThat(contentLikeCount(viewerId, contentId)).isEqualTo(1);
    PublicContentItem liked = publicContentQueryService.content(viewer, contentId);
    assertThat(liked.likeCount()).isEqualTo(1L);
    assertThat(liked.liked()).isTrue();
    assertThat(liked.saved()).isFalse();

    contentInteractionService.unlike(viewer, contentId);
    contentInteractionService.unlike(viewer, contentId);

    PublicContentItem unliked = publicContentQueryService.content(viewer, contentId);
    assertThat(contentLikeCount(viewerId, contentId)).isZero();
    assertThat(unliked.likeCount()).isZero();
    assertThat(unliked.liked()).isFalse();
  }

  @Test
  void concurrentDuplicateLikeCreatesOneLogicalRelationship() throws Exception {
    UUID viewerId = insertUser("concurrent-like-viewer", "VIEWER", "ACTIVE");
    UUID creatorId = insertCreator("concurrent-like-creator");
    UUID contentId = insertContent(creatorId, "PUBLISHED", BASE_TIME);
    MinisPrincipal viewer = principal(viewerId, "VIEWER", "ACTIVE");

    List<String> results = runConcurrently(() -> contentInteractionService.like(viewer, contentId));

    assertThat(results).containsOnly("ok");
    assertThat(contentLikeCount(viewerId, contentId)).isEqualTo(1);
  }

  @Test
  void savePersistenceIsPrivateIdempotentAndDoesNotAffectLikeCounts() {
    UUID viewerId = insertUser("save-viewer", "VIEWER", "ACTIVE");
    UUID otherViewerId = insertUser("save-other-viewer", "VIEWER", "ACTIVE");
    UUID creatorId = insertCreator("save-creator");
    UUID contentId = insertContent(creatorId, "PUBLISHED", BASE_TIME);
    MinisPrincipal viewer = principal(viewerId, "VIEWER", "ACTIVE");

    contentInteractionService.save(viewer, contentId);
    contentInteractionService.save(viewer, contentId);
    insertSave(otherViewerId, contentId, BASE_TIME.plusSeconds(1));

    assertThat(contentSaveCount(viewerId, contentId)).isEqualTo(1);
    assertThat(allSaveCount(contentId)).isEqualTo(2);
    PublicContentItem saved = publicContentQueryService.content(viewer, contentId);
    assertThat(saved.saved()).isTrue();
    assertThat(saved.likeCount()).isZero();

    contentInteractionService.unsave(viewer, contentId);
    contentInteractionService.unsave(viewer, contentId);

    PublicContentItem unsaved = publicContentQueryService.content(viewer, contentId);
    assertThat(contentSaveCount(viewerId, contentId)).isZero();
    assertThat(allSaveCount(contentId)).isEqualTo(1);
    assertThat(unsaved.saved()).isFalse();
  }

  @Test
  void concurrentDuplicateSaveCreatesOneLogicalRelationship() throws Exception {
    UUID viewerId = insertUser("concurrent-save-viewer", "VIEWER", "ACTIVE");
    UUID creatorId = insertCreator("concurrent-save-creator");
    UUID contentId = insertContent(creatorId, "PUBLISHED", BASE_TIME);
    MinisPrincipal viewer = principal(viewerId, "VIEWER", "ACTIVE");

    List<String> results = runConcurrently(() -> contentInteractionService.save(viewer, contentId));

    assertThat(results).containsOnly("ok");
    assertThat(contentSaveCount(viewerId, contentId)).isEqualTo(1);
  }

  @Test
  void databaseUniqueConstraintsRejectDirectDuplicateRelationships() {
    UUID viewerId = insertUser("unique-viewer", "VIEWER", "ACTIVE");
    UUID creatorId = insertCreator("unique-creator");
    UUID contentId = insertContent(creatorId, "PUBLISHED", BASE_TIME);
    insertLike(viewerId, contentId, BASE_TIME);
    insertSave(viewerId, contentId, BASE_TIME);

    assertThatThrownBy(() -> insertLike(viewerId, contentId, BASE_TIME.plusSeconds(1)))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> insertSave(viewerId, contentId, BASE_TIME.plusSeconds(1)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void newLikeOrSaveAgainstRemovedOrMissingContentFailsButDeleteRemainsIdempotent() {
    UUID viewerId = insertUser("removed-viewer", "VIEWER", "ACTIVE");
    UUID creatorId = insertCreator("removed-creator");
    UUID removedContentId = insertContent(creatorId, "REMOVED", BASE_TIME);
    UUID missingContentId = UUID.randomUUID();
    MinisPrincipal viewer = principal(viewerId, "VIEWER", "ACTIVE");

    assertThatThrownBy(() -> contentInteractionService.like(viewer, removedContentId))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.errorCode()).isEqualTo(ApiErrorCode.RESOURCE_NOT_FOUND));
    assertThatThrownBy(() -> contentInteractionService.save(viewer, removedContentId))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.errorCode()).isEqualTo(ApiErrorCode.RESOURCE_NOT_FOUND));
    assertThatThrownBy(() -> contentInteractionService.like(viewer, missingContentId))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.errorCode()).isEqualTo(ApiErrorCode.RESOURCE_NOT_FOUND));

    contentInteractionService.unlike(viewer, removedContentId);
    contentInteractionService.unlike(viewer, missingContentId);
    contentInteractionService.unsave(viewer, removedContentId);
    contentInteractionService.unsave(viewer, missingContentId);

    assertThat(contentLikeCount(viewerId, removedContentId)).isZero();
    assertThat(contentSaveCount(viewerId, removedContentId)).isZero();
  }

  @Test
  void likedCollectionUsesStableCursorOrderingAndExcludesRemovedContent() {
    UUID viewerId = insertUser("liked-page-viewer", "VIEWER", "ACTIVE");
    UUID otherViewerId = insertUser("liked-page-other-viewer", "VIEWER", "ACTIVE");
    UUID creatorId = insertCreator("liked-page-creator");
    UUID oldest = insertContent(creatorId, "PUBLISHED", BASE_TIME.minusSeconds(30));
    UUID tieA = insertContent(creatorId, "PUBLISHED", BASE_TIME.minusSeconds(20));
    UUID tieB = insertContent(creatorId, "PUBLISHED", BASE_TIME.minusSeconds(10));
    UUID newest = insertContent(creatorId, "PUBLISHED", BASE_TIME);
    UUID removed = insertContent(creatorId, "REMOVED", BASE_TIME.plusSeconds(10));
    insertLike(viewerId, oldest, BASE_TIME);
    insertLike(viewerId, tieA, BASE_TIME.plusSeconds(60));
    insertLike(viewerId, tieB, BASE_TIME.plusSeconds(60));
    insertLike(viewerId, newest, BASE_TIME.plusSeconds(120));
    insertLike(viewerId, removed, BASE_TIME.plusSeconds(180));
    insertLike(
        otherViewerId,
        insertContent(creatorId, "PUBLISHED", BASE_TIME.plusSeconds(20)),
        BASE_TIME.plusSeconds(240));
    MinisPrincipal viewer = principal(viewerId, "VIEWER", "ACTIVE");

    ContentPage first = publicContentQueryService.likedContent(viewer, null, 2);
    ContentPage second = publicContentQueryService.likedContent(viewer, first.nextCursor(), 2);
    List<UUID> pagedIds = new ArrayList<>();
    pagedIds.addAll(ids(first));
    pagedIds.addAll(ids(second));

    assertThat(first.items().get(0).id()).isEqualTo(newest);
    assertThat(pagedIds).containsExactlyInAnyOrder(newest, tieA, tieB, oldest);
    assertThat(first.nextCursor()).isNotBlank();
    assertThat(second.nextCursor()).isNull();
    assertThat(ids(first)).doesNotContainAnyElementsOf(ids(second));
    assertThat(ids(first)).doesNotContain(removed);
    assertThat(ids(second)).doesNotContain(removed);
    assertThat(first.items()).allMatch(PublicContentItem::liked);

    contentInteractionService.unlike(viewer, newest);

    ContentPage updated = publicContentQueryService.likedContent(viewer, null, 10);
    assertThat(updated.items()).extracting(PublicContentItem::id).doesNotContain(newest);
  }

  @Test
  void savedCollectionIsPrivateCursorPaginatedAndClampsPageSize() {
    UUID viewerId = insertUser("saved-page-viewer", "VIEWER", "ACTIVE");
    UUID otherViewerId = insertUser("saved-page-other-viewer", "VIEWER", "ACTIVE");
    UUID creatorId = insertCreator("saved-page-creator");
    List<UUID> viewerContentIds = new ArrayList<>();
    for (int index = 0; index < 35; index++) {
      UUID contentId = insertContent(creatorId, "PUBLISHED", BASE_TIME.plusSeconds(index));
      viewerContentIds.add(contentId);
      insertSave(viewerId, contentId, BASE_TIME.plusSeconds(index));
    }
    UUID otherContentId = insertContent(creatorId, "PUBLISHED", BASE_TIME.plusSeconds(40));
    UUID removedContentId = insertContent(creatorId, "REMOVED", BASE_TIME.plusSeconds(41));
    insertSave(otherViewerId, otherContentId, BASE_TIME.plusSeconds(40));
    insertSave(viewerId, removedContentId, BASE_TIME.plusSeconds(41));
    MinisPrincipal viewer = principal(viewerId, "VIEWER", "ACTIVE");

    ContentPage oversized = publicContentQueryService.savedContent(viewer, null, 100);
    ContentPage first = publicContentQueryService.savedContent(viewer, null, 1);
    ContentPage second = publicContentQueryService.savedContent(viewer, first.nextCursor(), 1);
    ContentPage zero = publicContentQueryService.savedContent(viewer, null, 0);
    ContentPage negative = publicContentQueryService.savedContent(viewer, null, -5);

    assertThat(oversized.limit()).isEqualTo(30);
    assertThat(oversized.items()).hasSize(30);
    assertThat(first.limit()).isEqualTo(1);
    assertThat(second.limit()).isEqualTo(1);
    assertThat(zero.limit()).isEqualTo(1);
    assertThat(negative.limit()).isEqualTo(1);
    assertThat(ids(first)).doesNotContainAnyElementsOf(ids(second));
    assertThat(oversized.items()).extracting(PublicContentItem::id).doesNotContain(otherContentId);
    assertThat(oversized.items())
        .extracting(PublicContentItem::id)
        .doesNotContain(removedContentId);
    assertThat(oversized.items())
        .extracting(PublicContentItem::id)
        .containsAnyElementsOf(viewerContentIds);
    assertThat(oversized.items()).allMatch(PublicContentItem::saved);

    contentInteractionService.unsave(viewer, first.items().get(0).id());

    ContentPage updated = publicContentQueryService.savedContent(viewer, null, 30);
    assertThat(updated.items())
        .extracting(PublicContentItem::id)
        .doesNotContain(first.items().get(0).id());
  }

  @Test
  void invalidPersonalCollectionCursorIsRejectedSafely() {
    UUID viewerId = insertUser("invalid-cursor-viewer", "VIEWER", "ACTIVE");

    assertThatThrownBy(
            () ->
                publicContentQueryService.likedContent(
                    principal(viewerId, "VIEWER", "ACTIVE"), "not-a-cursor", 10))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.errorCode()).isEqualTo(ApiErrorCode.VALIDATION_ERROR));
  }

  @Test
  void guestAndAuthenticatedProjectionStatesRemainConsistent() {
    UUID viewerId = insertUser("projection-viewer", "VIEWER", "ACTIVE");
    UUID otherViewerId = insertUser("projection-other-viewer", "VIEWER", "ACTIVE");
    UUID creatorA = insertCreator("projection-creator-a");
    UUID creatorB = insertCreator("projection-creator-b");
    UUID contentA = insertContent(creatorA, "PUBLISHED", BASE_TIME.plusSeconds(1), "HEALTH");
    UUID contentB = insertContent(creatorB, "PUBLISHED", BASE_TIME, "HEALTH");
    insertLike(viewerId, contentA, BASE_TIME);
    insertSave(viewerId, contentA, BASE_TIME);
    insertFollow(viewerId, creatorA, BASE_TIME);
    insertLike(otherViewerId, contentA, BASE_TIME);
    MinisPrincipal viewer = principal(viewerId, "VIEWER", "ACTIVE");

    ContentPage guest = publicContentQueryService.feed(null, null, 10, "HEALTH");
    ContentPage authenticated = publicContentQueryService.feed(viewer, null, 10, "HEALTH");

    PublicContentItem guestA = item(guest, contentA);
    PublicContentItem authenticatedA = item(authenticated, contentA);
    PublicContentItem authenticatedB = item(authenticated, contentB);
    assertThat(guestA.likeCount()).isEqualTo(2L);
    assertThat(guestA.liked()).isFalse();
    assertThat(guestA.saved()).isFalse();
    assertThat(guestA.following()).isFalse();
    assertThat(authenticatedA.liked()).isTrue();
    assertThat(authenticatedA.saved()).isTrue();
    assertThat(authenticatedA.following()).isTrue();
    assertThat(authenticatedB.liked()).isFalse();
    assertThat(authenticatedB.saved()).isFalse();
    assertThat(authenticatedB.following()).isFalse();
  }

  @Test
  void historicalFollowStillProjectsForPublishedContentAfterCreatorRevocation() {
    UUID viewerId = insertUser("historical-follow-viewer", "VIEWER", "ACTIVE");
    UUID creatorId = insertCreator("historical-follow-creator");
    UUID contentId = insertContent(creatorId, "PUBLISHED", BASE_TIME);
    insertFollow(viewerId, creatorId, BASE_TIME);
    jdbcTemplate.update("update user_accounts set role = 'VIEWER' where id = ?", creatorId);

    PublicContentItem content =
        publicContentQueryService.content(principal(viewerId, "VIEWER", "ACTIVE"), contentId);

    assertThat(content.following()).isTrue();
  }

  private List<String> runConcurrently(CheckedRunnable operation) throws Exception {
    ExecutorService executorService = Executors.newFixedThreadPool(4);
    CyclicBarrier barrier = new CyclicBarrier(4);
    List<Callable<String>> tasks = new ArrayList<>();
    for (int index = 0; index < 4; index++) {
      tasks.add(
          () -> {
            barrier.await();
            operation.run();
            return "ok";
          });
    }
    try {
      List<Future<String>> futures = executorService.invokeAll(tasks);
      return futures.stream().map(ContentInteractionPersistenceIT::getUnchecked).toList();
    } finally {
      executorService.shutdownNow();
    }
  }

  private UUID insertUser(String label, String role, String status) {
    UUID userId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        insert into user_accounts (
          id, firebase_uid, email, email_verified, display_name, role, status,
          created_at, updated_at, last_authenticated_at
        ) values (?, ?, ?, true, ?, ?, ?, current_timestamp, current_timestamp, current_timestamp)
        """,
        userId,
        label + "-" + userId,
        label + "@example.com",
        label,
        role,
        status);
    return userId;
  }

  private UUID insertCreator(String label) {
    UUID creatorId = insertUser(label, "CREATOR", "ACTIVE");
    jdbcTemplate.update(
        """
        insert into creator_profiles (
          user_id, public_slug, public_display_name, verified, created_at, updated_at
        ) values (?, ?, ?, true, current_timestamp, current_timestamp)
        """,
        creatorId,
        label + "-" + creatorId.toString().substring(0, 8),
        "Creator " + label);
    return creatorId;
  }

  private UUID insertContent(UUID creatorId, String status, Instant publishedAt) {
    return insertContent(creatorId, status, publishedAt, "EDUCATION");
  }

  private UUID insertContent(UUID creatorId, String status, Instant publishedAt, String category) {
    UUID contentId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        insert into content_items (
          id,
          creator_user_id,
          title,
          description,
          category,
          media_object_key,
          media_content_type,
          media_size_bytes,
          duration_ms,
          status,
          import_source_key,
          created_at,
          published_at,
          removed_at,
          removal_reason
        ) values (?, ?, 'Title', 'Description', ?, ?, 'video/mp4', 1024, 1000, ?, ?,
          current_timestamp, ?, case when ? = 'REMOVED' then current_timestamp else null end,
          case when ? = 'REMOVED' then 'admin removal' else null end)
        """,
        contentId,
        creatorId,
        category,
        "content/imports/" + contentId + ".mp4",
        status,
        "source-" + contentId,
        Timestamp.from(publishedAt),
        status,
        status);
    return contentId;
  }

  private void insertLike(UUID userId, UUID contentId, Instant createdAt) {
    jdbcTemplate.update(
        "insert into content_likes (user_id, content_id, created_at) values (?, ?, ?)",
        userId,
        contentId,
        Timestamp.from(createdAt));
  }

  private void insertSave(UUID userId, UUID contentId, Instant createdAt) {
    jdbcTemplate.update(
        "insert into content_saves (user_id, content_id, created_at) values (?, ?, ?)",
        userId,
        contentId,
        Timestamp.from(createdAt));
  }

  private void insertFollow(UUID followerId, UUID creatorId, Instant createdAt) {
    jdbcTemplate.update(
        "insert into creator_follows (follower_user_id, creator_user_id, created_at) values (?, ?, ?)",
        followerId,
        creatorId,
        Timestamp.from(createdAt));
  }

  private long contentLikeCount(UUID userId, UUID contentId) {
    return relationshipCount("content_likes", "user_id", "content_id", userId, contentId);
  }

  private long contentSaveCount(UUID userId, UUID contentId) {
    return relationshipCount("content_saves", "user_id", "content_id", userId, contentId);
  }

  private long allSaveCount(UUID contentId) {
    Long count =
        jdbcTemplate.queryForObject(
            "select count(*) from content_saves where content_id = ?", Long.class, contentId);
    return count == null ? 0L : count;
  }

  private long relationshipCount(
      String tableName, String userColumn, String targetColumn, UUID userId, UUID targetId) {
    Long count =
        jdbcTemplate.queryForObject(
            "select count(*) from "
                + tableName
                + " where "
                + userColumn
                + " = ? and "
                + targetColumn
                + " = ?",
            Long.class,
            userId,
            targetId);
    return count == null ? 0L : count;
  }

  private static MinisPrincipal principal(UUID userId, String role, String status) {
    return new MinisPrincipal(
        TestIdentities.googleIdentity(userId.toString()), userId, role, status);
  }

  private static List<UUID> ids(ContentPage page) {
    return page.items().stream().map(PublicContentItem::id).toList();
  }

  private static PublicContentItem item(ContentPage page, UUID contentId) {
    return page.items().stream()
        .filter(content -> content.id().equals(contentId))
        .findFirst()
        .orElseThrow();
  }

  private static String getUnchecked(Future<String> future) {
    try {
      return future.get();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Interrupted while waiting for content interaction", exception);
    } catch (java.util.concurrent.ExecutionException exception) {
      throw new AssertionError("Content interaction failed", exception);
    }
  }

  @FunctionalInterface
  private interface CheckedRunnable {
    void run() throws Exception;
  }
}
