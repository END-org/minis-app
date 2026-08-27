package com.end.minisbackend.content.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.end.minisbackend.content.application.ContentPage;
import com.end.minisbackend.content.application.ContentStorage;
import com.end.minisbackend.content.application.PublicContentQueryService;
import com.end.minisbackend.content.application.SignedReadUrl;
import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class ContentPersistenceIT {
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private PublicContentQueryService publicContentQueryService;

  @MockitoBean private ContentStorage contentStorage;

  @Test
  void feedUsesStableCursorOrderingAndExcludesRemovedContent() {
    when(contentStorage.signedReadUrl(anyString()))
        .thenReturn(
            new SignedReadUrl(
                URI.create("http://localhost:9000/minis-event-media/content/imports/test.mp4"),
                Instant.parse("2026-07-17T12:10:00Z")));
    UUID creatorId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    insertCreator(creatorId, "feed-creator");
    UUID oldest = UUID.fromString("11111111-1111-1111-1111-111111111111");
    UUID tieWinner = UUID.fromString("22222222-2222-2222-2222-222222222222");
    UUID newest = UUID.fromString("33333333-3333-3333-3333-333333333333");
    insertContent(oldest, creatorId, "source-oldest", "PUBLISHED", "2026-07-17T12:00:00Z");
    insertContent(tieWinner, creatorId, "source-tie", "PUBLISHED", "2026-07-17T12:00:00Z");
    insertContent(newest, creatorId, "source-newest", "PUBLISHED", "2026-07-17T12:01:00Z");
    insertContent(
        UUID.fromString("44444444-4444-4444-4444-444444444444"),
        creatorId,
        "source-removed",
        "REMOVED",
        "2026-07-17T12:02:00Z");

    ContentPage first = publicContentQueryService.feed(null, 2, null);
    ContentPage second = publicContentQueryService.feed(first.nextCursor(), 2, null);

    assertThat(first.items()).extracting(item -> item.id()).containsExactly(newest, tieWinner);
    assertThat(second.items()).extracting(item -> item.id()).containsExactly(oldest);
    assertThat(first.items().get(0).media().url()).startsWith("http://localhost:9000/");
    assertThat(first.nextCursor()).isNotBlank();
  }

  @Test
  void creatorContentUsesCreatorVisibilityAndCategoryFilteringWorks() {
    when(contentStorage.signedReadUrl(anyString()))
        .thenReturn(
            new SignedReadUrl(
                URI.create("http://localhost:9000/minis-event-media/content/imports/test.mp4"),
                Instant.parse("2026-07-17T12:10:00Z")));
    UUID creatorId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    insertCreator(creatorId, "category-creator");
    insertContent(
        UUID.fromString("55555555-5555-5555-5555-555555555555"),
        creatorId,
        "source-education",
        "PUBLISHED",
        "2026-07-17T12:00:00Z");
    insertContent(
        UUID.fromString("66666666-6666-6666-6666-666666666666"),
        creatorId,
        "source-technology",
        "PUBLISHED",
        "2026-07-17T12:01:00Z",
        "TECHNOLOGY");

    ContentPage creatorContent =
        publicContentQueryService.creatorContent("category-creator", null, 10);
    ContentPage technology = publicContentQueryService.feed(null, 10, "TECHNOLOGY");

    assertThat(creatorContent.items()).hasSize(2);
    assertThat(technology.items()).hasSize(1);
    assertThat(technology.items().get(0).category()).isEqualTo("TECHNOLOGY");
  }

  private void insertCreator(UUID userId, String slug) {
    jdbcTemplate.update(
        """
        insert into user_accounts (
          id, firebase_uid, email_verified, role, status, created_at, updated_at, last_authenticated_at
        ) values (?, ?, true, 'CREATOR', 'ACTIVE', current_timestamp, current_timestamp, current_timestamp)
        """,
        userId,
        "firebase-" + slug);
    jdbcTemplate.update(
        """
        insert into creator_profiles (
          user_id, public_slug, public_display_name, verified, created_at, updated_at
        ) values (?, ?, 'Event Creator', true, current_timestamp, current_timestamp)
        """,
        userId,
        slug);
  }

  private void insertContent(
      UUID contentId, UUID creatorId, String sourceKey, String status, String publishedAt) {
    insertContent(contentId, creatorId, sourceKey, status, publishedAt, "EDUCATION");
  }

  private void insertContent(
      UUID contentId,
      UUID creatorId,
      String sourceKey,
      String status,
      String publishedAt,
      String category) {
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
        "content/imports/" + sourceKey + ".mp4",
        status,
        sourceKey,
        Timestamp.from(Instant.parse(publishedAt)),
        status,
        status);
  }
}
