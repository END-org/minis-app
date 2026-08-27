package com.end.minisbackend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.end.minisbackend.TestIdentities;
import com.end.minisbackend.admin.application.AdminUserPage;
import com.end.minisbackend.admin.application.EventAdminService;
import com.end.minisbackend.content.application.ContentAdministrationService;
import com.end.minisbackend.content.application.ContentStorage;
import com.end.minisbackend.content.application.SignedReadUrl;
import com.end.minisbackend.identity.application.FirebaseTokenVerifier;
import com.end.minisbackend.identity.application.LocalUserIdentity;
import com.end.minisbackend.identity.application.LocalUserIdentityGateway;
import com.end.minisbackend.identity.application.MinisPrincipal;
import com.end.minisbackend.identity.application.ProvisionedUserAccount;
import com.end.minisbackend.identity.application.UserAccountProvisioningGateway;
import com.end.minisbackend.identity.domain.VerifiedFirebaseIdentity;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import com.end.minisbackend.shared.security.CorrelationId;
import com.end.minisbackend.shared.security.MinisAuthenticationException;
import com.end.minisbackend.user.application.CurrentUserService;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "minis.http.allowed-origins=http://localhost:5173")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class SecurityIntegrationTest {
  private static final String LOCAL_ORIGIN = "http://localhost:5173";
  private static final String DEMO_ORIGIN = "http://localhost:8088";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private FirebaseTokenVerifier firebaseTokenVerifier;
  @MockitoBean private LocalUserIdentityGateway localUserIdentityGateway;
  @MockitoBean private UserAccountProvisioningGateway userAccountProvisioningGateway;
  @MockitoBean private CurrentUserService currentUserService;
  @MockitoBean private EventAdminService eventAdminService;
  @MockitoBean private ContentAdministrationService contentAdministrationService;
  @MockitoBean private ContentStorage contentStorage;

  @BeforeEach
  void setUp() {
    when(localUserIdentityGateway.findByFirebaseUid(any())).thenReturn(Optional.empty());
    when(contentStorage.signedReadUrl(any()))
        .thenReturn(
            new SignedReadUrl(
                URI.create("http://localhost:9000/minis-event-media/content/imports/test.mp4"),
                Instant.parse("2026-07-17T12:10:00Z")));
  }

  @Test
  void healthEndpointIsPublic() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void readinessHealthEndpointIsPublic() throws Exception {
    mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
  }

  @Test
  void publicCreatorsEndpointAllowsGuestAccess() throws Exception {
    mockMvc.perform(get("/api/v1/creators")).andExpect(status().isOk());
    verifyNoInteractions(firebaseTokenVerifier);
  }

  @Test
  void publicContentDiscoveryEndpointsAllowGuestAccess() throws Exception {
    mockMvc.perform(get("/api/v1/categories")).andExpect(status().isOk());
    mockMvc.perform(get("/api/v1/feed")).andExpect(status().isOk());
    verifyNoInteractions(firebaseTokenVerifier);
  }

  @Test
  void protectedMeRequiresBearerToken() throws Exception {
    mockMvc
        .perform(get("/api/v1/me").header(CorrelationId.HEADER_NAME, "phase-zero-correlation"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(CorrelationId.HEADER_NAME, "phase-zero-correlation"))
        .andExpect(jsonPath("$.code", is("MISSING_AUTH_TOKEN")))
        .andExpect(jsonPath("$.correlationId", is("phase-zero-correlation")));
  }

  @Test
  void localCorsAllowsBootstrapPostWithAuthorizationAndCorrelationHeader() throws Exception {
    VerifiedFirebaseIdentity identity = registerToken("cors-bootstrap-user", Optional.empty());
    when(userAccountProvisioningGateway.bootstrap(identity))
        .thenReturn(provisionedAccount("cors-bootstrap-user"));

    mockMvc
        .perform(
            post("/api/v1/auth/bootstrap")
                .header(HttpHeaders.ORIGIN, LOCAL_ORIGIN)
                .header(CorrelationId.HEADER_NAME, "cors-bootstrap-correlation")
                .header(HttpHeaders.AUTHORIZATION, bearer("cors-bootstrap-user-token")))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, LOCAL_ORIGIN))
        .andExpect(
            header()
                .string(
                    HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                    containsString(CorrelationId.HEADER_NAME)))
        .andExpect(header().string(CorrelationId.HEADER_NAME, "cors-bootstrap-correlation"))
        .andExpect(jsonPath("$.role", is("VIEWER")));
  }

  @Test
  void localCorsPreflightAllowsPostAuthorizationAndCorrelationHeaders() throws Exception {
    mockMvc
        .perform(
            options("/api/v1/auth/bootstrap")
                .header(HttpHeaders.ORIGIN, LOCAL_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(
                    HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                    HttpHeaders.AUTHORIZATION + ", " + CorrelationId.HEADER_NAME))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, LOCAL_ORIGIN))
        .andExpect(
            header()
                .string(
                    HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                    containsString(HttpHeaders.AUTHORIZATION)))
        .andExpect(
            header()
                .string(
                    HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                    containsString(CorrelationId.HEADER_NAME)));
  }

  @Test
  void localCorsRejectsUnapprovedOriginAndLogsReason(CapturedOutput output) throws Exception {
    mockMvc
        .perform(
            options("/api/v1/auth/bootstrap")
                .header(HttpHeaders.ORIGIN, "http://localhost:5174")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(
                    HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                    HttpHeaders.AUTHORIZATION + ", " + CorrelationId.HEADER_NAME))
        .andExpect(status().isForbidden())
        .andExpect(content().string("Invalid CORS request"));

    assertThat(output)
        .contains("event=cors_request_rejected")
        .contains("origin=http://localhost:5174")
        .contains("allowedOrigins=[http://localhost:5173]")
        .contains("rejectionCategory=origin_not_allowed");
  }

  @Test
  void localCorsDoesNotAcceptDemoOriginWhenOnlyLocalOriginIsConfigured() throws Exception {
    mockMvc
        .perform(
            options("/api/v1/auth/bootstrap")
                .header(HttpHeaders.ORIGIN, DEMO_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
        .andExpect(status().isForbidden());
  }

  @Test
  void malformedBearerHeaderIsRejected() throws Exception {
    mockMvc
        .perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Basic abc"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code", is("INVALID_AUTH_TOKEN")));
  }

  @Test
  void invalidTokenIsRejected(CapturedOutput output) throws Exception {
    when(firebaseTokenVerifier.verify("missing"))
        .thenThrow(new MinisAuthenticationException(ApiErrorCode.INVALID_AUTH_TOKEN));

    mockMvc
        .perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer missing"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code", is("INVALID_AUTH_TOKEN")));
    assertThat(output)
        .contains("event=firebase_authentication_failed")
        .contains("event=security_authentication_failed")
        .contains("apiErrorCode=INVALID_AUTH_TOKEN")
        .doesNotContain("Bearer missing");
  }

  @Test
  void preBootstrapPrincipalCanCallBootstrap() throws Exception {
    VerifiedFirebaseIdentity identity = registerToken("bootstrap-user", Optional.empty());
    when(userAccountProvisioningGateway.bootstrap(identity))
        .thenReturn(provisionedAccount("bootstrap-user"));

    mockMvc
        .perform(
            post("/api/v1/auth/bootstrap")
                .header(HttpHeaders.AUTHORIZATION, bearer("bootstrap-user-token")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role", is("VIEWER")))
        .andExpect(jsonPath("$.status", is("ACTIVE")));
  }

  @Test
  void bootstrapRefreshesCurrentAuthenticationContextAfterExistingAdminPromotion(
      CapturedOutput output) throws Exception {
    UUID userId = UUID.randomUUID();
    VerifiedFirebaseIdentity identity =
        registerToken(
            "allowlisted-existing-admin",
            Optional.of(new LocalUserIdentity(userId, "VIEWER", "ACTIVE")));
    when(userAccountProvisioningGateway.bootstrap(identity))
        .thenReturn(provisionedAccount("allowlisted-existing-admin", userId, "ADMIN"));

    mockMvc
        .perform(
            post("/api/v1/auth/bootstrap")
                .header(CorrelationId.HEADER_NAME, "bootstrap-promoted-context")
                .header(HttpHeaders.AUTHORIZATION, bearer("allowlisted-existing-admin-token")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role", is("ADMIN")))
        .andExpect(jsonPath("$.status", is("ACTIVE")));

    String completionLine = lineContaining(output.toString(), "bootstrap-promoted-context");
    assertThat(completionLine)
        .contains("event=http_request_completed")
        .contains("localUserId=" + userId)
        .contains("role=ADMIN");
  }

  @Test
  void unverifiedPasswordBootstrapReturnsForbidden() throws Exception {
    VerifiedFirebaseIdentity identity =
        TestIdentities.emailPasswordIdentity("unverified-bootstrap-user", false);
    when(firebaseTokenVerifier.verify("unverified-bootstrap-user-token")).thenReturn(identity);
    when(localUserIdentityGateway.findByFirebaseUid("unverified-bootstrap-user"))
        .thenReturn(Optional.empty());

    mockMvc
        .perform(
            post("/api/v1/auth/bootstrap")
                .header(HttpHeaders.AUTHORIZATION, bearer("unverified-bootstrap-user-token")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code", is("EMAIL_NOT_VERIFIED")));
  }

  @Test
  void preBootstrapPrincipalCannotCallMe() throws Exception {
    registerToken("not-provisioned-user", Optional.empty());
    when(currentUserService.currentUser(any(MinisPrincipal.class)))
        .thenThrow(new ApiException(ApiErrorCode.ACCOUNT_NOT_PROVISIONED));

    mockMvc
        .perform(
            get("/api/v1/me")
                .header(HttpHeaders.AUTHORIZATION, bearer("not-provisioned-user-token")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code", is("ACCOUNT_NOT_PROVISIONED")));
  }

  @Test
  void activeViewerCanCallMe() throws Exception {
    registerToken(
        "active-viewer", Optional.of(new LocalUserIdentity(UUID.randomUUID(), "VIEWER", "ACTIVE")));
    when(currentUserService.currentUser(any(MinisPrincipal.class)))
        .thenReturn(provisionedAccount("active-viewer"));

    mockMvc
        .perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer("active-viewer-token")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role", is("VIEWER")))
        .andExpect(jsonPath("$.status", is("ACTIVE")));
  }

  @Test
  void viewerCannotCallAdminRoutes(CapturedOutput output) throws Exception {
    LocalUserIdentity viewer = new LocalUserIdentity(UUID.randomUUID(), "VIEWER", "ACTIVE");
    registerToken("admin-blocked-viewer", Optional.of(viewer));

    mockMvc
        .perform(
            get("/api/v1/admin/users")
                .header(HttpHeaders.AUTHORIZATION, bearer("admin-blocked-viewer-token")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));
    verifyNoInteractions(eventAdminService);
    assertThat(output)
        .contains("event=security_access_denied")
        .contains("localUserId=" + viewer.id())
        .contains("role=VIEWER")
        .contains("apiErrorCode=ACCESS_DENIED");
  }

  @Test
  void creatorCannotCallAdminRoutes() throws Exception {
    registerToken(
        "admin-blocked-creator",
        Optional.of(new LocalUserIdentity(UUID.randomUUID(), "CREATOR", "ACTIVE")));

    mockMvc
        .perform(
            get("/api/v1/admin/users")
                .header(HttpHeaders.AUTHORIZATION, bearer("admin-blocked-creator-token")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));
    verifyNoInteractions(eventAdminService);
  }

  @Test
  void adminCanReachAdminRoutes() throws Exception {
    UUID adminId = UUID.randomUUID();
    registerToken("event-admin", Optional.of(new LocalUserIdentity(adminId, "ADMIN", "ACTIVE")));
    when(eventAdminService.users(any(MinisPrincipal.class), any(), anyInt(), anyInt()))
        .thenReturn(new AdminUserPage(List.of(), 0, 20, false));

    mockMvc
        .perform(
            get("/api/v1/admin/users")
                .header(HttpHeaders.AUTHORIZATION, bearer("event-admin-token")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.users").isArray());
  }

  @Test
  void guestCannotRemoveContent() throws Exception {
    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                    "/api/v1/admin/content/{contentId}/removal", UUID.randomUUID())
                .contentType("application/json")
                .content("{\"reason\":\"event cleanup\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code", is("MISSING_AUTH_TOKEN")));
    verifyNoInteractions(contentAdministrationService);
  }

  @Test
  void viewerCannotRemoveContent() throws Exception {
    registerToken(
        "content-removal-viewer",
        Optional.of(new LocalUserIdentity(UUID.randomUUID(), "VIEWER", "ACTIVE")));

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                    "/api/v1/admin/content/{contentId}/removal", UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, bearer("content-removal-viewer-token"))
                .contentType("application/json")
                .content("{\"reason\":\"event cleanup\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));
    verifyNoInteractions(contentAdministrationService);
  }

  @Test
  void creatorCannotRemoveContent() throws Exception {
    registerToken(
        "content-removal-creator",
        Optional.of(new LocalUserIdentity(UUID.randomUUID(), "CREATOR", "ACTIVE")));

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                    "/api/v1/admin/content/{contentId}/removal", UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, bearer("content-removal-creator-token"))
                .contentType("application/json")
                .content("{\"reason\":\"event cleanup\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));
    verifyNoInteractions(contentAdministrationService);
  }

  @Test
  void adminCanRemoveContent() throws Exception {
    UUID adminId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();
    registerToken(
        "content-removal-admin", Optional.of(new LocalUserIdentity(adminId, "ADMIN", "ACTIVE")));

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                    "/api/v1/admin/content/{contentId}/removal", contentId)
                .header(HttpHeaders.AUTHORIZATION, bearer("content-removal-admin-token"))
                .contentType("application/json")
                .content("{\"reason\":\"event cleanup\"}"))
        .andExpect(status().isNoContent());
  }

  @Test
  void suspendedUserCannotCallProtectedCurrentUserEndpoint() throws Exception {
    registerToken(
        "suspended-viewer",
        Optional.of(new LocalUserIdentity(UUID.randomUUID(), "VIEWER", "SUSPENDED")));

    mockMvc
        .perform(
            get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer("suspended-viewer-token")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code", is("ACCOUNT_SUSPENDED")));
    verifyNoInteractions(currentUserService);
  }

  @Test
  void disabledUserCannotCallProtectedCurrentUserEndpoint() throws Exception {
    registerToken(
        "disabled-viewer",
        Optional.of(new LocalUserIdentity(UUID.randomUUID(), "VIEWER", "DISABLED")));

    mockMvc
        .perform(
            get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer("disabled-viewer-token")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code", is("ACCOUNT_DISABLED")));
    verifyNoInteractions(currentUserService);
  }

  @Test
  void requestCompletionLogIsProducedOnceWithCorrelationStatusAndDuration(CapturedOutput output)
      throws Exception {
    mockMvc
        .perform(
            get("/api/v1/creators").header(CorrelationId.HEADER_NAME, "completion-correlation"))
        .andExpect(status().isOk());

    assertThat(output).contains("event=http_request_completed");
    assertThat(occurrences(output.toString(), "event=http_request_completed")).isEqualTo(1);
    assertThat(output)
        .contains("correlationId=completion-correlation")
        .contains("status=200")
        .contains("durationMs=");
  }

  @Test
  void unexpectedRuntimeFailureLogsOneErrorWithoutSensitiveValues(CapturedOutput output)
      throws Exception {
    registerToken(
        "runtime-failure-user",
        Optional.of(new LocalUserIdentity(UUID.randomUUID(), "VIEWER", "ACTIVE")));
    when(currentUserService.currentUser(any(MinisPrincipal.class)))
        .thenThrow(new RuntimeException("simulated failure"));

    mockMvc
        .perform(
            get("/api/v1/me")
                .header(CorrelationId.HEADER_NAME, "runtime-failure-correlation")
                .header(HttpHeaders.AUTHORIZATION, bearer("runtime-failure-user-token")))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code", is("INTERNAL_ERROR")));

    assertThat(output)
        .contains("event=api_request_failed")
        .contains("correlationId=runtime-failure-correlation")
        .contains("exceptionCategory=RuntimeException")
        .doesNotContain("runtime-failure-user-token")
        .doesNotContain("Bearer runtime-failure-user-token");
  }

  @Test
  void expectedUnauthorizedErrorDoesNotEmitStackTrace(CapturedOutput output) throws Exception {
    mockMvc
        .perform(get("/api/v1/me").header(CorrelationId.HEADER_NAME, "missing-auth-correlation"))
        .andExpect(status().isUnauthorized());

    assertThat(linesContaining(output.toString(), "missing-auth-correlation"))
        .contains("event=security_authentication_failed")
        .doesNotContain("java.lang.RuntimeException")
        .doesNotContain("\tat com.end.minisbackend");
  }

  @Test
  void authenticatedMdcStateDoesNotLeakToLaterGuestRequests(CapturedOutput output)
      throws Exception {
    UUID viewerId = UUID.randomUUID();
    registerToken("mdc-viewer", Optional.of(new LocalUserIdentity(viewerId, "VIEWER", "ACTIVE")));
    when(currentUserService.currentUser(any(MinisPrincipal.class)))
        .thenReturn(provisionedAccount("mdc-viewer"));

    mockMvc
        .perform(
            get("/api/v1/me")
                .header(CorrelationId.HEADER_NAME, "mdc-authenticated")
                .header(HttpHeaders.AUTHORIZATION, bearer("mdc-viewer-token")))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/v1/creators").header(CorrelationId.HEADER_NAME, "mdc-guest"))
        .andExpect(status().isOk());

    String guestCompletionLine = lineContaining(output.toString(), "correlationId=mdc-guest");
    assertThat(guestCompletionLine).contains("localUserId=-").contains("role=-");
    assertThat(guestCompletionLine).doesNotContain(viewerId.toString());
  }

  private VerifiedFirebaseIdentity registerToken(
      String firebaseUid, Optional<LocalUserIdentity> localUserIdentity) {
    String token = firebaseUid + "-token";
    VerifiedFirebaseIdentity identity = TestIdentities.emailPasswordIdentity(firebaseUid, true);
    when(firebaseTokenVerifier.verify(token)).thenReturn(identity);
    when(localUserIdentityGateway.findByFirebaseUid(firebaseUid)).thenReturn(localUserIdentity);
    return identity;
  }

  private static ProvisionedUserAccount provisionedAccount(String firebaseUid) {
    return provisionedAccount(firebaseUid, UUID.randomUUID(), "VIEWER");
  }

  private static ProvisionedUserAccount provisionedAccount(
      String firebaseUid, UUID userId, String role) {
    return new ProvisionedUserAccount(
        userId,
        firebaseUid + "@example.com",
        true,
        "Firebase " + firebaseUid,
        null,
        null,
        role,
        "ACTIVE",
        Instant.parse("2026-07-17T12:00:00Z"));
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }

  private static int occurrences(String text, String value) {
    int count = 0;
    int index = text.indexOf(value);
    while (index >= 0) {
      count++;
      index = text.indexOf(value, index + value.length());
    }
    return count;
  }

  private static String lineContaining(String text, String value) {
    return text.lines().filter(line -> line.contains(value)).findFirst().orElse("");
  }

  private static String linesContaining(String text, String value) {
    return String.join(
        System.lineSeparator(), text.lines().filter(line -> line.contains(value)).toList());
  }
}
