package com.end.minisbackend.content.infrastructure.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.end.minisbackend.TestIdentities;
import com.end.minisbackend.content.application.model.feed.ContentPage;
import com.end.minisbackend.content.application.model.feed.SignedReadUrl;
import com.end.minisbackend.content.application.model.upload.ContentUploadPage;
import com.end.minisbackend.content.application.port.ContentCommandStore;
import com.end.minisbackend.content.application.port.ContentStorage;
import com.end.minisbackend.content.application.service.feed.PublicContentQueryService;
import com.end.minisbackend.content.application.service.upload.ContentUploadQueryService;
import com.end.minisbackend.content.domain.model.ContentItem;
import com.end.minisbackend.identity.application.model.MinisPrincipal;
import java.net.URI;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class ContentUploadPersistenceIT {
  private static final Instant BASE_TIME = Instant.parse("2099-07-18T12:00:00Z");
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private ContentUploadQueryService contentUploadQueryService;
  @Autowired private PublicContentQueryService publicContentQueryService;
  @Autowired private ContentCommandStore contentCommandStore;

  @MockitoBean private ContentStorage contentStorage;

  @Test
  void creatorUploadsArePrivatePaginatedAndExcludedFromPublicFeedUntilPublished() {
    when(contentStorage.signedReadUrl(anyString()))
        .thenReturn(
            new SignedReadUrl(
                URI.create("http://localhost:9000/minis-event-media/content/uploads/test.mp4"),
                BASE_TIME.plus(Duration.ofMinutes(10))));
    UUID creatorId = UUID.randomUUID();
    UUID otherCreatorId = UUID.randomUUID();
    insertCreator(creatorId, "upload-private-" + creatorId.toString().substring(0, 8));
    insertCreator(otherCreatorId, "upload-other-" + otherCreatorId.toString().substring(0, 8));
    UUID uploading =
        insertUpload(creatorId, "upload-private-uploading", "UPLOADING", BASE_TIME.plusSeconds(3));
    UUID failed =
        insertUpload(creatorId, "upload-private-failed", "FAILED", BASE_TIME.plusSeconds(2));
    UUID published =
        insertUpload(creatorId, "upload-private-published", "PUBLISHED", BASE_TIME.plusSeconds(1));
    insertUpload(otherCreatorId, "upload-other-published", "PUBLISHED", BASE_TIME.plusSeconds(4));

    ContentUploadPage firstPage = contentUploadQueryService.uploads(principal(creatorId), null, 2);
    ContentUploadPage secondPage =
        contentUploadQueryService.uploads(principal(creatorId), firstPage.nextCursor(), 2);
    ContentPage feed = publicContentQueryService.feed(principal(creatorId), null, 10, "EDUCATION");
    List<ContentItem> stale =
        contentCommandStore.findStaleUploadingItemsBefore(
            BASE_TIME.minus(Duration.ofMinutes(30)), 10);

    assertThat(firstPage.items()).extracting(item -> item.id()).containsExactly(uploading, failed);
    assertThat(firstPage.nextCursor()).isNotBlank();
    assertThat(secondPage.items()).extracting(item -> item.id()).containsExactly(published);
    assertThat(secondPage.items().get(0).content()).isNotNull();
    assertThat(feed.items()).extracting(item -> item.id()).contains(published);
    assertThat(feed.items()).extracting(item -> item.id()).doesNotContain(uploading, failed);
    assertThat(stale).extracting(ContentItem::id).contains(uploading);
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
        ) values (?, ?, 'Upload Creator', true, current_timestamp, current_timestamp)
        """,
        userId,
        slug);
  }

  private UUID insertUpload(UUID creatorId, String keySuffix, String status, Instant createdAt) {
    UUID contentId = UUID.randomUUID();
    Instant uploadStartedAt =
        "UPLOADING".equals(status) ? BASE_TIME.minus(Duration.ofHours(1)) : createdAt;
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
          removal_reason,
          upload_idempotency_key,
          upload_started_at,
          upload_completed_at,
          upload_failure_code,
          upload_failure_reason
        ) values (?, ?, ?, 'Upload description', 'EDUCATION', ?, ?, ?, ?, ?, null, ?, ?, null, null,
          ?, ?, ?, ?, ?)
        """,
        contentId,
        creatorId,
        "Upload " + keySuffix,
        "content/uploads/" + contentId + ".mp4",
        "PUBLISHED".equals(status) ? "video/mp4" : null,
        "PUBLISHED".equals(status) ? 1024L : null,
        "PUBLISHED".equals(status) ? 60_000L : null,
        status,
        Timestamp.from(createdAt),
        "PUBLISHED".equals(status) ? Timestamp.from(createdAt.plusSeconds(10)) : null,
        keySuffix,
        Timestamp.from(uploadStartedAt),
        "UPLOADING".equals(status) ? null : Timestamp.from(createdAt.plusSeconds(10)),
        "FAILED".equals(status) ? "UNSUPPORTED_MEDIA_PROFILE" : null,
        "FAILED".equals(status) ? "The video does not match the MINIS event media rules." : null);
    return contentId;
  }

  private static MinisPrincipal principal(UUID creatorId) {
    return new MinisPrincipal(
        TestIdentities.googleIdentity("upload-query-" + creatorId), creatorId, "CREATOR", "ACTIVE");
  }
}
