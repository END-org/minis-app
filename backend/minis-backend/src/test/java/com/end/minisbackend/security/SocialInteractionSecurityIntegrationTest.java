package com.end.minisbackend.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.end.minisbackend.TestIdentities;
import com.end.minisbackend.content.application.ContentStorage;
import com.end.minisbackend.content.application.SignedReadUrl;
import com.end.minisbackend.identity.infrastructure.FakeFirebaseTokenVerifier;
import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SocialInteractionSecurityIntegrationTest {
  private static final Instant BASE_TIME = Instant.parse("2026-07-18T12:00:00Z");

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private FakeFirebaseTokenVerifier tokenVerifier;

  @MockitoBean private ContentStorage contentStorage;

  @BeforeEach
  void setUp() {
    tokenVerifier.clear();
    when(contentStorage.signedReadUrl(anyString()))
        .thenReturn(
            new SignedReadUrl(
                URI.create("http://localhost:9000/minis-event-media/content/imports/test.mp4"),
                Instant.parse("2026-07-18T12:10:00Z")));
  }

  @Test
  void guestCannotWriteSocialRelationshipsOrReadPrivateCollections() throws Exception {
    CreatorFixture creator = insertCreator("guest-social-creator", "CREATOR", "ACTIVE");
    UUID contentId = insertContent(creator.userId(), "PUBLISHED");

    mockMvc
        .perform(put("/api/v1/content/{contentId}/like", contentId))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code", is("MISSING_AUTH_TOKEN")));
    mockMvc
        .perform(put("/api/v1/content/{contentId}/save", contentId))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code", is("MISSING_AUTH_TOKEN")));
    mockMvc
        .perform(put("/api/v1/creators/{slug}/follow", creator.slug()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code", is("MISSING_AUTH_TOKEN")));
    mockMvc
        .perform(get("/api/v1/me/liked-content"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code", is("MISSING_AUTH_TOKEN")));
    mockMvc
        .perform(get("/api/v1/me/saved-content"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code", is("MISSING_AUTH_TOKEN")));
  }

  @Test
  void invalidTokenIsRejectedForSocialWrite() throws Exception {
    CreatorFixture creator = insertCreator("invalid-token-creator", "CREATOR", "ACTIVE");
    UUID contentId = insertContent(creator.userId(), "PUBLISHED");

    mockMvc
        .perform(
            put("/api/v1/content/{contentId}/like", contentId)
                .header(HttpHeaders.AUTHORIZATION, bearer("not-registered")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code", is("INVALID_AUTH_TOKEN")));
  }

  @Test
  void activeViewerCanLikeSaveFollowAndReadOnlyOwnPrivateCollections() throws Exception {
    UserFixture viewer = insertAuthenticatedUser("active-social-viewer", "VIEWER", "ACTIVE");
    UserFixture otherViewer = insertAuthenticatedUser("active-social-other", "VIEWER", "ACTIVE");
    CreatorFixture creator = insertCreator("active-social-creator", "CREATOR", "ACTIVE");
    UUID contentId = insertContent(creator.userId(), "PUBLISHED");

    mockMvc
        .perform(
            put("/api/v1/content/{contentId}/like", contentId)
                .header(HttpHeaders.AUTHORIZATION, bearer(viewer.token())))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(
            put("/api/v1/content/{contentId}/save", contentId)
                .header(HttpHeaders.AUTHORIZATION, bearer(viewer.token())))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(
            put("/api/v1/creators/{slug}/follow", creator.slug())
                .header(HttpHeaders.AUTHORIZATION, bearer(viewer.token())))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get("/api/v1/content/{contentId}", contentId)
                .header(HttpHeaders.AUTHORIZATION, bearer(viewer.token())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.likeCount", is(1)))
        .andExpect(jsonPath("$.liked", is(true)))
        .andExpect(jsonPath("$.saved", is(true)))
        .andExpect(jsonPath("$.following", is(true)))
        .andExpect(content().string(not(containsString(viewer.firebaseUid()))))
        .andExpect(content().string(not(containsString(viewer.token()))));

    mockMvc
        .perform(
            get("/api/v1/me/saved-content")
                .header(HttpHeaders.AUTHORIZATION, bearer(viewer.token())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()", is(1)))
        .andExpect(jsonPath("$.items[0].id", is(contentId.toString())))
        .andExpect(jsonPath("$.items[0].saved", is(true)));
    mockMvc
        .perform(
            get("/api/v1/me/saved-content")
                .header(HttpHeaders.AUTHORIZATION, bearer(otherViewer.token())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()", is(0)));
  }

  @Test
  void activeCreatorCanUseNormalViewerInteractions() throws Exception {
    UserFixture actingCreator =
        insertAuthenticatedUser("acting-social-creator", "CREATOR", "ACTIVE");
    insertCreatorProfile(
        actingCreator.id(),
        "acting-social-creator-" + actingCreator.id().toString().substring(0, 8));
    CreatorFixture targetCreator = insertCreator("target-social-creator", "CREATOR", "ACTIVE");
    UUID contentId = insertContent(targetCreator.userId(), "PUBLISHED");

    mockMvc
        .perform(
            put("/api/v1/content/{contentId}/like", contentId)
                .header(HttpHeaders.AUTHORIZATION, bearer(actingCreator.token())))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(
            put("/api/v1/content/{contentId}/save", contentId)
                .header(HttpHeaders.AUTHORIZATION, bearer(actingCreator.token())))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(
            put("/api/v1/creators/{slug}/follow", targetCreator.slug())
                .header(HttpHeaders.AUTHORIZATION, bearer(actingCreator.token())))
        .andExpect(status().isNoContent());
  }

  @Test
  void suspendedAndDisabledUsersCannotInteract() throws Exception {
    UserFixture suspended =
        insertAuthenticatedUser("suspended-social-viewer", "VIEWER", "SUSPENDED");
    UserFixture disabled = insertAuthenticatedUser("disabled-social-viewer", "VIEWER", "DISABLED");
    CreatorFixture creator = insertCreator("blocked-social-creator", "CREATOR", "ACTIVE");
    UUID contentId = insertContent(creator.userId(), "PUBLISHED");

    mockMvc
        .perform(
            put("/api/v1/content/{contentId}/like", contentId)
                .header(HttpHeaders.AUTHORIZATION, bearer(suspended.token())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code", is("ACCOUNT_SUSPENDED")));
    mockMvc
        .perform(
            put("/api/v1/content/{contentId}/save", contentId)
                .header(HttpHeaders.AUTHORIZATION, bearer(suspended.token())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code", is("ACCOUNT_SUSPENDED")));
    mockMvc
        .perform(
            put("/api/v1/creators/{slug}/follow", creator.slug())
                .header(HttpHeaders.AUTHORIZATION, bearer(suspended.token())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code", is("ACCOUNT_SUSPENDED")));
    mockMvc
        .perform(
            put("/api/v1/content/{contentId}/like", contentId)
                .header(HttpHeaders.AUTHORIZATION, bearer(disabled.token())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code", is("ACCOUNT_DISABLED")));
  }

  @Test
  void selfFollowAndNonCreatorFollowAreRejected() throws Exception {
    UserFixture creator = insertAuthenticatedUser("self-social-creator", "CREATOR", "ACTIVE");
    String selfSlug =
        insertCreatorProfile(
            creator.id(), "self-social-creator-" + creator.id().toString().substring(0, 8));
    UserFixture viewer = insertAuthenticatedUser("non-creator-social-viewer", "VIEWER", "ACTIVE");
    UserFixture target = insertAuthenticatedUser("non-creator-social-target", "VIEWER", "ACTIVE");
    String nonCreatorSlug =
        insertCreatorProfile(
            target.id(), "non-creator-social-target-" + target.id().toString().substring(0, 8));

    mockMvc
        .perform(
            put("/api/v1/creators/{slug}/follow", selfSlug)
                .header(HttpHeaders.AUTHORIZATION, bearer(creator.token())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code", is("RESOURCE_CONFLICT")));
    mockMvc
        .perform(
            put("/api/v1/creators/{slug}/follow", nonCreatorSlug)
                .header(HttpHeaders.AUTHORIZATION, bearer(viewer.token())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")));
  }

  @Test
  void removedMissingAndMalformedContentTargetsAreHandledSafely() throws Exception {
    UserFixture viewer = insertAuthenticatedUser("target-social-viewer", "VIEWER", "ACTIVE");
    CreatorFixture creator = insertCreator("target-social-creator", "CREATOR", "ACTIVE");
    UUID removedContentId = insertContent(creator.userId(), "REMOVED");
    UUID missingContentId = UUID.randomUUID();

    mockMvc
        .perform(
            put("/api/v1/content/{contentId}/like", removedContentId)
                .header(HttpHeaders.AUTHORIZATION, bearer(viewer.token())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")));
    mockMvc
        .perform(
            put("/api/v1/content/{contentId}/save", missingContentId)
                .header(HttpHeaders.AUTHORIZATION, bearer(viewer.token())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")));
    mockMvc
        .perform(
            delete("/api/v1/content/{contentId}/like", removedContentId)
                .header(HttpHeaders.AUTHORIZATION, bearer(viewer.token())))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(
            put("/api/v1/content/not-a-uuid/like")
                .header(HttpHeaders.AUTHORIZATION, bearer(viewer.token())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
  }

  @Test
  void guestAndAuthenticatedFeedHaveSameContractWithDifferentViewerState() throws Exception {
    UserFixture viewer = insertAuthenticatedUser("feed-state-viewer", "VIEWER", "ACTIVE");
    CreatorFixture creator = insertCreator("feed-state-creator", "CREATOR", "ACTIVE");
    UUID contentId = insertContent(creator.userId(), "PUBLISHED", "SCIENCE");
    jdbcTemplate.update(
        "insert into content_likes (user_id, content_id, created_at) values (?, ?, ?)",
        viewer.id(),
        contentId,
        Timestamp.from(BASE_TIME));

    mockMvc
        .perform(get("/api/v1/feed").param("category", "SCIENCE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].likeCount", is(1)))
        .andExpect(jsonPath("$.items[0].liked", is(false)))
        .andExpect(jsonPath("$.items[0].saved", is(false)))
        .andExpect(jsonPath("$.items[0].following", is(false)));
    mockMvc
        .perform(
            get("/api/v1/feed")
                .param("category", "SCIENCE")
                .header(HttpHeaders.AUTHORIZATION, bearer(viewer.token())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].likeCount", is(1)))
        .andExpect(jsonPath("$.items[0].liked", is(true)))
        .andExpect(jsonPath("$.items[0].saved", is(false)))
        .andExpect(jsonPath("$.items[0].following", is(false)));
  }

  private UserFixture insertAuthenticatedUser(String label, String role, String status) {
    UUID userId = UUID.randomUUID();
    String firebaseUid = label + "-" + userId;
    String token = label + "-token-" + userId;
    jdbcTemplate.update(
        """
        insert into user_accounts (
          id, firebase_uid, email, email_verified, display_name, role, status,
          created_at, updated_at, last_authenticated_at
        ) values (?, ?, ?, true, ?, ?, ?, current_timestamp, current_timestamp, current_timestamp)
        """,
        userId,
        firebaseUid,
        label + "@example.com",
        label,
        role,
        status);
    tokenVerifier.register(token, TestIdentities.googleIdentity(firebaseUid));
    return new UserFixture(userId, firebaseUid, token);
  }

  private CreatorFixture insertCreator(String label, String role, String status) {
    UserFixture user = insertAuthenticatedUser(label, role, status);
    String slug =
        insertCreatorProfile(user.id(), label + "-" + user.id().toString().substring(0, 8));
    return new CreatorFixture(user.id(), slug);
  }

  private String insertCreatorProfile(UUID userId, String slug) {
    jdbcTemplate.update(
        """
        insert into creator_profiles (
          user_id, public_slug, public_display_name, verified, created_at, updated_at
        ) values (?, ?, ?, true, current_timestamp, current_timestamp)
        """,
        userId,
        slug,
        "Creator " + slug);
    return slug;
  }

  private UUID insertContent(UUID creatorId, String status) {
    return insertContent(creatorId, status, "EDUCATION");
  }

  private UUID insertContent(UUID creatorId, String status, String category) {
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
        Timestamp.from(BASE_TIME),
        status,
        status);
    return contentId;
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }

  private record UserFixture(UUID id, String firebaseUid, String token) {}

  private record CreatorFixture(UUID userId, String slug) {}
}
