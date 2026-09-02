package com.end.minisbackend.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.end.minisbackend.TestIdentities;
import com.end.minisbackend.content.application.ContentStorage;
import com.end.minisbackend.content.application.MediaInspection;
import com.end.minisbackend.content.application.MediaProbe;
import com.end.minisbackend.content.application.SignedReadUrl;
import com.end.minisbackend.identity.infrastructure.FakeFirebaseTokenVerifier;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContentUploadSecurityIntegrationTest {
  private static final Instant BASE_TIME = Instant.parse("2026-07-18T12:00:00Z");
  private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private FakeFirebaseTokenVerifier tokenVerifier;

  @MockitoBean private ContentStorage contentStorage;
  @MockitoBean private MediaProbe mediaProbe;

  @BeforeEach
  void setUp() {
    tokenVerifier.clear();
    reset(contentStorage, mediaProbe);
    when(mediaProbe.inspect(any(Path.class))).thenReturn(validInspection());
    when(contentStorage.signedReadUrl(anyString()))
        .thenReturn(
            new SignedReadUrl(
                URI.create("http://localhost:9000/minis-event-media/content/uploads/test.mp4"),
                BASE_TIME.plusSeconds(600)));
  }

  @Test
  void guestCannotUploadOrReadCreatorUploads() throws Exception {
    mockMvc
        .perform(validMultipart("guest-upload-1"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code", is("MISSING_AUTH_TOKEN")));
    mockMvc
        .perform(get("/api/v1/me/uploads"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code", is("MISSING_AUTH_TOKEN")));
  }

  @Test
  void viewerSuspendedDisabledAndAdminAccountsCannotUpload() throws Exception {
    UserFixture viewer = insertAuthenticatedUser("upload-viewer", "VIEWER", "ACTIVE");
    UserFixture suspendedCreator =
        insertAuthenticatedUser("upload-suspended-creator", "CREATOR", "SUSPENDED");
    UserFixture disabledCreator =
        insertAuthenticatedUser("upload-disabled-creator", "CREATOR", "DISABLED");
    UserFixture admin = insertAuthenticatedUser("upload-admin", "ADMIN", "ACTIVE");

    mockMvc
        .perform(
            validMultipart("viewer-upload-1")
                .header(HttpHeaders.AUTHORIZATION, bearer(viewer.token())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));
    mockMvc
        .perform(
            validMultipart("suspended-upload-1")
                .header(HttpHeaders.AUTHORIZATION, bearer(suspendedCreator.token())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code", is("ACCOUNT_SUSPENDED")));
    mockMvc
        .perform(
            validMultipart("disabled-upload-1")
                .header(HttpHeaders.AUTHORIZATION, bearer(disabledCreator.token())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code", is("ACCOUNT_DISABLED")));
    mockMvc
        .perform(
            validMultipart("admin-upload-1")
                .header(HttpHeaders.AUTHORIZATION, bearer(admin.token())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));
  }

  @Test
  void activeCreatorUploadPublishesWithoutLeakingPrivateIdentity() throws Exception {
    UserFixture creator = insertAuthenticatedUser("upload-active-creator", "CREATOR", "ACTIVE");
    insertCreatorProfile(
        creator.id(), "upload-active-creator-" + creator.id().toString().substring(0, 8));

    mockMvc
        .perform(
            validMultipart("creator-upload-security-1")
                .header(HttpHeaders.AUTHORIZATION, bearer(creator.token())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status", is("PUBLISHED")))
        .andExpect(jsonPath("$.content.id").exists())
        .andExpect(jsonPath("$.content.media.url").exists())
        .andExpect(content().string(not(containsString(creator.firebaseUid()))))
        .andExpect(content().string(not(containsString(creator.token()))));

    Map<String, Object> row = uploadRow(creator.id(), "creator-upload-security-1");
    assertPublishedUploadRow(row);
    verify(contentStorage)
        .uploadObject(startsWith("content/uploads/"), any(Path.class), eq("video/mp4"), eq(16L));
  }

  @Test
  void successfulReplayReturnsExistingUploadWithoutStoringAgain() throws Exception {
    UserFixture creator = insertAuthenticatedUser("upload-replay-creator", "CREATOR", "ACTIVE");
    insertCreatorProfile(
        creator.id(), "upload-replay-creator-" + creator.id().toString().substring(0, 8));

    mockMvc
        .perform(
            validMultipart("creator-upload-replay-1")
                .header(HttpHeaders.AUTHORIZATION, bearer(creator.token())))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            validMultipart("creator-upload-replay-1")
                .header(HttpHeaders.AUTHORIZATION, bearer(creator.token())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status", is("PUBLISHED")));

    verify(contentStorage, times(1))
        .uploadObject(startsWith("content/uploads/"), any(Path.class), eq("video/mp4"), eq(16L));
  }

  @Test
  void invalidContentTypePersistsSafeFailedUploadForCreator() throws Exception {
    UserFixture creator = insertAuthenticatedUser("upload-invalid-creator", "CREATOR", "ACTIVE");
    insertCreatorProfile(
        creator.id(), "upload-invalid-creator-" + creator.id().toString().substring(0, 8));

    mockMvc
        .perform(
            multipart("/api/v1/content")
                .file(metadataPart())
                .file(new MockMultipartFile("video", "clip.mp4", "video/quicktime", mp4Bytes()))
                .header(IDEMPOTENCY_KEY_HEADER, "creator-upload-invalid-1")
                .header(HttpHeaders.AUTHORIZATION, bearer(creator.token())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code", is("UPLOAD_VALIDATION_FAILED")));
    mockMvc
        .perform(
            get("/api/v1/me/uploads").header(HttpHeaders.AUTHORIZATION, bearer(creator.token())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].status", is("FAILED")))
        .andExpect(jsonPath("$.items[0].failureCode", is("UNSUPPORTED_CONTENT_TYPE")))
        .andExpect(jsonPath("$.items[0].content").doesNotExist())
        .andExpect(content().string(not(containsString("video/quicktime"))));
  }

  @Test
  void creatorCannotReadAnotherCreatorsUploadHistory() throws Exception {
    UserFixture owner = insertAuthenticatedUser("upload-owner", "CREATOR", "ACTIVE");
    insertCreatorProfile(owner.id(), "upload-owner-" + owner.id().toString().substring(0, 8));
    UserFixture otherCreator = insertAuthenticatedUser("upload-other", "CREATOR", "ACTIVE");
    insertCreatorProfile(
        otherCreator.id(), "upload-other-" + otherCreator.id().toString().substring(0, 8));

    mockMvc
        .perform(
            validMultipart("owner-upload-1")
                .header(HttpHeaders.AUTHORIZATION, bearer(owner.token())))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            get("/api/v1/me/uploads")
                .header(HttpHeaders.AUTHORIZATION, bearer(otherCreator.token())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isEmpty());
  }

  private MockMultipartHttpServletRequestBuilder validMultipart(String idempotencyKey) {
    return multipart("/api/v1/content")
        .file(metadataPart())
        .file(new MockMultipartFile("video", "clip.mp4", "video/mp4", mp4Bytes()))
        .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey);
  }

  private static MockMultipartFile metadataPart() {
    return new MockMultipartFile(
        "metadata",
        "metadata.json",
        MediaType.APPLICATION_JSON_VALUE,
        """
        {
          "title": "Upload title",
          "description": "Upload description",
          "category": "EDUCATION"
        }
        """
            .getBytes(StandardCharsets.UTF_8));
  }

  private static byte[] mp4Bytes() {
    return "0000ftypisom0000".getBytes(StandardCharsets.US_ASCII);
  }

  private static MediaInspection validInspection() {
    return new MediaInspection(
        "mov,mp4,m4a,3gp,3g2,mj2", "h264", "aac", 60_000, 1080, 1920, 30d, 2);
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

  private void insertCreatorProfile(UUID userId, String slug) {
    jdbcTemplate.update(
        """
        insert into creator_profiles (
          user_id, public_slug, public_display_name, verified, created_at, updated_at
        ) values (?, ?, ?, true, current_timestamp, current_timestamp)
        """,
        userId,
        slug,
        "Creator " + slug);
  }

  private Map<String, Object> uploadRow(UUID creatorId, String idempotencyKey) {
    return jdbcTemplate.queryForMap(
        """
        select status, media_object_key, media_content_type, media_size_bytes, import_source_key,
               upload_completed_at, upload_failure_code
        from content_items
        where creator_user_id = ?
          and upload_idempotency_key = ?
        """,
        creatorId,
        idempotencyKey);
  }

  private static void assertPublishedUploadRow(Map<String, Object> row) {
    org.assertj.core.api.Assertions.assertThat(row.get("status")).isEqualTo("PUBLISHED");
    org.assertj.core.api.Assertions.assertThat(row.get("media_object_key").toString())
        .startsWith("content/uploads/")
        .endsWith(".mp4");
    org.assertj.core.api.Assertions.assertThat(row.get("media_content_type"))
        .isEqualTo("video/mp4");
    org.assertj.core.api.Assertions.assertThat(row.get("media_size_bytes")).isEqualTo(16L);
    org.assertj.core.api.Assertions.assertThat(row.get("import_source_key")).isNull();
    org.assertj.core.api.Assertions.assertThat(row.get("upload_completed_at")).isNotNull();
    org.assertj.core.api.Assertions.assertThat(row.get("upload_failure_code")).isNull();
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }

  private record UserFixture(UUID id, String firebaseUid, String token) {}
}
