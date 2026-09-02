package com.end.minisbackend.content.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.end.minisbackend.TestIdentities;
import com.end.minisbackend.TestTransactionManager;
import com.end.minisbackend.content.application.model.feed.ContentCursor;
import com.end.minisbackend.content.application.model.feed.ContentPage;
import com.end.minisbackend.content.application.model.feed.PersonalContentView;
import com.end.minisbackend.content.application.model.feed.PublishedContentView;
import com.end.minisbackend.content.application.model.interaction.ContentInteractionCursor;
import com.end.minisbackend.content.application.port.ContentInteractionStore;
import com.end.minisbackend.content.application.port.ContentQueryStore;
import com.end.minisbackend.content.application.service.feed.PublicContentAssembler;
import com.end.minisbackend.content.application.service.feed.PublicContentQueryService;
import com.end.minisbackend.content.domain.enums.ContentCategory;
import com.end.minisbackend.creator.application.port.CreatorFollowStore;
import com.end.minisbackend.creator.application.service.read.CreatorProfileQueryService;
import com.end.minisbackend.identity.application.model.MinisPrincipal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PublicContentQueryServiceTest {
  private static final UUID VIEWER_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
  private static final UUID CONTENT_A = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
  private static final UUID CONTENT_B = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
  private static final UUID CREATOR_A = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
  private static final UUID CREATOR_B = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

  @Test
  void authenticatedFeedLoadsSocialStateWithOneBatchPerRelationshipKind() {
    RecordingContentQueryStore contentQueryStore =
        new RecordingContentQueryStore(
            List.of(content(CONTENT_A, CREATOR_A), content(CONTENT_B, CREATOR_B)));
    RecordingContentInteractionStore contentInteractionStore =
        new RecordingContentInteractionStore(Set.of(CONTENT_A), Set.of(CONTENT_A));
    RecordingCreatorFollowStore creatorFollowStore =
        new RecordingCreatorFollowStore(Set.of(CREATOR_A));
    PublicContentQueryService service =
        service(contentQueryStore, contentInteractionStore, creatorFollowStore);

    ContentPage page = service.feed(principal(), null, 10, null);

    assertThat(page.items()).hasSize(2);
    assertThat(page.items().get(0).likeCount()).isEqualTo(2L);
    assertThat(page.items().get(0).liked()).isTrue();
    assertThat(page.items().get(0).saved()).isTrue();
    assertThat(page.items().get(0).following()).isTrue();
    assertThat(page.items().get(1).likeCount()).isZero();
    assertThat(page.items().get(1).liked()).isFalse();
    assertThat(page.items().get(1).saved()).isFalse();
    assertThat(page.items().get(1).following()).isFalse();
    assertThat(contentInteractionStore.likeCountsQueries()).isEqualTo(1);
    assertThat(contentInteractionStore.likedContentQueries()).isEqualTo(1);
    assertThat(contentInteractionStore.savedContentQueries()).isEqualTo(1);
    assertThat(creatorFollowStore.followedCreatorIdsQueries()).isEqualTo(1);
    assertThat(contentInteractionStore.lastContentIds()).containsExactly(CONTENT_A, CONTENT_B);
    assertThat(creatorFollowStore.lastCreatorUserIds()).containsExactly(CREATOR_A, CREATOR_B);
  }

  @Test
  void guestFeedKeepsStableShapeWithoutViewerSpecificLookups() {
    RecordingContentQueryStore contentQueryStore =
        new RecordingContentQueryStore(List.of(content(CONTENT_A, CREATOR_A)));
    RecordingContentInteractionStore contentInteractionStore =
        new RecordingContentInteractionStore(Set.of(CONTENT_A), Set.of(CONTENT_A));
    RecordingCreatorFollowStore creatorFollowStore =
        new RecordingCreatorFollowStore(Set.of(CREATOR_A));
    PublicContentQueryService service =
        service(contentQueryStore, contentInteractionStore, creatorFollowStore);

    ContentPage page = service.feed(null, null, 10, null);

    assertThat(page.items()).hasSize(1);
    assertThat(page.items().get(0).likeCount()).isEqualTo(2L);
    assertThat(page.items().get(0).liked()).isFalse();
    assertThat(page.items().get(0).saved()).isFalse();
    assertThat(page.items().get(0).following()).isFalse();
    assertThat(contentInteractionStore.likeCountsQueries()).isEqualTo(1);
    assertThat(contentInteractionStore.likedContentQueries()).isZero();
    assertThat(contentInteractionStore.savedContentQueries()).isZero();
    assertThat(creatorFollowStore.followedCreatorIdsQueries()).isZero();
  }

  private static PublicContentQueryService service(
      ContentQueryStore contentQueryStore,
      ContentInteractionStore contentInteractionStore,
      CreatorFollowStore creatorFollowStore) {
    return new PublicContentQueryService(
        contentQueryStore,
        contentInteractionStore,
        new CreatorProfileQueryService(
            new InMemoryCreatorProfileStore(CREATOR_A, "creator-a"),
            creatorFollowStore,
            new TestTransactionManager()),
        new PublicContentAssembler(
            contentInteractionStore, new RecordingContentStorage(), creatorFollowStore),
        new TestTransactionManager());
  }

  private static MinisPrincipal principal() {
    return new MinisPrincipal(
        TestIdentities.googleIdentity("viewer"), VIEWER_ID, "VIEWER", "ACTIVE");
  }

  private static PublishedContentView content(UUID contentId, UUID creatorUserId) {
    return new PublishedContentView(
        contentId,
        creatorUserId,
        "creator-" + creatorUserId.toString().substring(35),
        "Creator",
        null,
        true,
        "Title",
        "Description",
        ContentCategory.EDUCATION,
        "content/imports/" + contentId + ".mp4",
        "video/mp4",
        1024L,
        1000L,
        Instant.parse("2026-07-18T12:00:00Z"));
  }

  private static final class RecordingContentQueryStore implements ContentQueryStore {
    private final List<PublishedContentView> rows;

    RecordingContentQueryStore(List<PublishedContentView> rows) {
      this.rows = List.copyOf(rows);
    }

    @Override
    public List<PublishedContentView> findFeed(
        ContentCategory category, ContentCursor cursor, int limit) {
      return rows.stream().limit(limit).toList();
    }

    @Override
    public List<PublishedContentView> findCreatorContent(
        UUID creatorUserId, ContentCursor cursor, int limit) {
      return rows.stream()
          .filter(row -> row.creatorUserId().equals(creatorUserId))
          .limit(limit)
          .toList();
    }

    @Override
    public Optional<PublishedContentView> findPublishedById(UUID contentId) {
      return rows.stream().filter(row -> row.id().equals(contentId)).findFirst();
    }
  }

  private static final class RecordingContentInteractionStore implements ContentInteractionStore {
    private final Set<UUID> likedContentIds;
    private final Set<UUID> savedContentIds;
    private int likeCountsQueries;
    private int likedContentQueries;
    private int savedContentQueries;
    private List<UUID> lastContentIds = List.of();

    RecordingContentInteractionStore(Set<UUID> likedContentIds, Set<UUID> savedContentIds) {
      this.likedContentIds = Set.copyOf(likedContentIds);
      this.savedContentIds = Set.copyOf(savedContentIds);
    }

    @Override
    public int ensureLikedIfPublished(UUID userId, UUID contentId, Instant createdAt) {
      return 0;
    }

    @Override
    public boolean isLiked(UUID userId, UUID contentId) {
      return likedContentIds.contains(contentId);
    }

    @Override
    public void removeLike(UUID userId, UUID contentId) {}

    @Override
    public int ensureSavedIfPublished(UUID userId, UUID contentId, Instant createdAt) {
      return 0;
    }

    @Override
    public boolean isSaved(UUID userId, UUID contentId) {
      return savedContentIds.contains(contentId);
    }

    @Override
    public void removeSave(UUID userId, UUID contentId) {}

    @Override
    public boolean publishedContentExists(UUID contentId) {
      return true;
    }

    @Override
    public Map<UUID, Long> likeCountsByContentIds(List<UUID> contentIds) {
      likeCountsQueries++;
      lastContentIds = List.copyOf(contentIds);
      return Map.of(CONTENT_A, 2L);
    }

    @Override
    public Set<UUID> likedContentIds(UUID userId, List<UUID> contentIds) {
      likedContentQueries++;
      return likedContentIds;
    }

    @Override
    public Set<UUID> savedContentIds(UUID userId, List<UUID> contentIds) {
      savedContentQueries++;
      return savedContentIds;
    }

    @Override
    public List<PersonalContentView> findLikedContent(
        UUID userId, ContentInteractionCursor cursor, int limit) {
      return List.of();
    }

    @Override
    public List<PersonalContentView> findSavedContent(
        UUID userId, ContentInteractionCursor cursor, int limit) {
      return List.of();
    }

    int likeCountsQueries() {
      return likeCountsQueries;
    }

    int likedContentQueries() {
      return likedContentQueries;
    }

    int savedContentQueries() {
      return savedContentQueries;
    }

    List<UUID> lastContentIds() {
      return lastContentIds;
    }
  }

  private static final class RecordingCreatorFollowStore implements CreatorFollowStore {
    private final Set<UUID> followedCreatorIds;
    private int followedCreatorIdsQueries;
    private List<UUID> lastCreatorUserIds = List.of();

    RecordingCreatorFollowStore(Set<UUID> followedCreatorIds) {
      this.followedCreatorIds = Set.copyOf(followedCreatorIds);
    }

    @Override
    public int ensureFollowIfCreatorIsPublic(
        UUID followerUserId, UUID creatorUserId, Instant createdAt) {
      return 0;
    }

    @Override
    public boolean isFollowing(UUID followerUserId, UUID creatorUserId) {
      return followedCreatorIds.contains(creatorUserId);
    }

    @Override
    public void removeFollow(UUID followerUserId, UUID creatorUserId) {}

    @Override
    public Optional<UUID> findCreatorUserIdBySlug(String slug) {
      return Optional.empty();
    }

    @Override
    public Set<UUID> followedCreatorIds(UUID followerUserId, List<UUID> creatorUserIds) {
      followedCreatorIdsQueries++;
      lastCreatorUserIds = List.copyOf(creatorUserIds);
      return followedCreatorIds;
    }

    int followedCreatorIdsQueries() {
      return followedCreatorIdsQueries;
    }

    List<UUID> lastCreatorUserIds() {
      return lastCreatorUserIds;
    }
  }
}
