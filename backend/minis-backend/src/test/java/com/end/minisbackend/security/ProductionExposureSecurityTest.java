package com.end.minisbackend.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.end.minisbackend.TestIdentities;
import com.end.minisbackend.identity.application.BootstrapCurrentIdentityService;
import com.end.minisbackend.identity.application.FirebaseTokenVerifier;
import com.end.minisbackend.identity.application.LocalUserIdentity;
import com.end.minisbackend.identity.application.LocalUserIdentityGateway;
import com.end.minisbackend.identity.application.UserAccountProvisioningGateway;
import com.end.minisbackend.user.application.CurrentUserService;
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

@SpringBootTest(
    properties = {"springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=true"})
@AutoConfigureMockMvc
@ActiveProfiles({"prod", "test"})
class ProductionExposureSecurityTest {
  @Autowired private MockMvc mockMvc;

  @MockitoBean private FirebaseTokenVerifier firebaseTokenVerifier;
  @MockitoBean private LocalUserIdentityGateway localUserIdentityGateway;
  @MockitoBean private BootstrapCurrentIdentityService bootstrapCurrentIdentityService;
  @MockitoBean private UserAccountProvisioningGateway userAccountProvisioningGateway;
  @MockitoBean private CurrentUserService currentUserService;

  @BeforeEach
  void setUp() {
    when(firebaseTokenVerifier.verify("prod-token"))
        .thenReturn(TestIdentities.emailPasswordIdentity("prod-user", true));
    when(localUserIdentityGateway.findByFirebaseUid(any()))
        .thenReturn(Optional.of(new LocalUserIdentity(UUID.randomUUID(), "VIEWER", "ACTIVE")));
  }

  @Test
  void productionProfileDeniesAuthTestPage() throws Exception {
    mockMvc
        .perform(get("/auth-test/index.html").header(HttpHeaders.AUTHORIZATION, bearer()))
        .andExpect(status().isForbidden());
  }

  @Test
  void productionProfileDeniesDeveloperFirebaseConfig() throws Exception {
    mockMvc
        .perform(get("/api/v1/dev/firebase-web-config").header(HttpHeaders.AUTHORIZATION, bearer()))
        .andExpect(status().isForbidden());
  }

  @Test
  void productionProfileDeniesSwaggerUiEvenWhenEnabled() throws Exception {
    mockMvc
        .perform(get("/swagger-ui/index.html").header(HttpHeaders.AUTHORIZATION, bearer()))
        .andExpect(status().isForbidden());
  }

  @Test
  void productionProfileDeniesApiDocumentationEvenWhenEnabled() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs").header(HttpHeaders.AUTHORIZATION, bearer()))
        .andExpect(status().isForbidden());
  }

  private static String bearer() {
    return "Bearer prod-token";
  }
}
