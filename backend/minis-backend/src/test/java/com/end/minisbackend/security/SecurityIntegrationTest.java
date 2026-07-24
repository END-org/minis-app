package com.end.minisbackend.security;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.end.minisbackend.TestIdentities;
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
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {
  @Autowired private MockMvc mockMvc;

  @MockitoBean private FirebaseTokenVerifier firebaseTokenVerifier;
  @MockitoBean private LocalUserIdentityGateway localUserIdentityGateway;
  @MockitoBean private UserAccountProvisioningGateway userAccountProvisioningGateway;
  @MockitoBean private CurrentUserService currentUserService;

  @BeforeEach
  void setUp() {
    when(localUserIdentityGateway.findByFirebaseUid(any())).thenReturn(Optional.empty());
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
  void protectedMeRequiresBearerToken() throws Exception {
    mockMvc
        .perform(get("/api/v1/me").header(CorrelationId.HEADER_NAME, "phase-zero-correlation"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(CorrelationId.HEADER_NAME, "phase-zero-correlation"))
        .andExpect(jsonPath("$.code", is("MISSING_AUTH_TOKEN")))
        .andExpect(jsonPath("$.correlationId", is("phase-zero-correlation")));
  }

  @Test
  void malformedBearerHeaderIsRejected() throws Exception {
    mockMvc
        .perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Basic abc"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code", is("INVALID_AUTH_TOKEN")));
  }

  @Test
  void invalidTokenIsRejected() throws Exception {
    when(firebaseTokenVerifier.verify("missing"))
        .thenThrow(new MinisAuthenticationException(ApiErrorCode.INVALID_AUTH_TOKEN));

    mockMvc
        .perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer missing"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code", is("INVALID_AUTH_TOKEN")));
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

  private VerifiedFirebaseIdentity registerToken(
      String firebaseUid, Optional<LocalUserIdentity> localUserIdentity) {
    String token = firebaseUid + "-token";
    VerifiedFirebaseIdentity identity = TestIdentities.emailPasswordIdentity(firebaseUid, true);
    when(firebaseTokenVerifier.verify(token)).thenReturn(identity);
    when(localUserIdentityGateway.findByFirebaseUid(firebaseUid)).thenReturn(localUserIdentity);
    return identity;
  }

  private static ProvisionedUserAccount provisionedAccount(String firebaseUid) {
    return new ProvisionedUserAccount(
        UUID.randomUUID(),
        firebaseUid + "@example.com",
        true,
        "Firebase " + firebaseUid,
        null,
        null,
        "VIEWER",
        "ACTIVE",
        Instant.parse("2026-07-17T12:00:00Z"));
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }
}
