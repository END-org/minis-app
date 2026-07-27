package com.end.minisbackend.security;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.end.minisbackend.admin.application.EventAdminService;
import com.end.minisbackend.identity.application.FirebaseTokenVerifier;
import com.end.minisbackend.identity.application.LocalUserIdentityGateway;
import com.end.minisbackend.identity.application.UserAccountProvisioningGateway;
import com.end.minisbackend.shared.security.CorrelationId;
import com.end.minisbackend.user.application.CurrentUserService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "minis.http.allowed-origins=http://localhost:8088")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemoCorsIntegrationTest {
  private static final String DEMO_ORIGIN = "http://localhost:8088";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private FirebaseTokenVerifier firebaseTokenVerifier;
  @MockitoBean private LocalUserIdentityGateway localUserIdentityGateway;
  @MockitoBean private UserAccountProvisioningGateway userAccountProvisioningGateway;
  @MockitoBean private CurrentUserService currentUserService;
  @MockitoBean private EventAdminService eventAdminService;

  @BeforeEach
  void setUp() {
    when(localUserIdentityGateway.findByFirebaseUid(any())).thenReturn(Optional.empty());
  }

  @Test
  void demoCorsAllowsDemoProxyOriginPreflight() throws Exception {
    mockMvc
        .perform(
            options("/api/v1/auth/bootstrap")
                .header(HttpHeaders.ORIGIN, DEMO_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(
                    HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                    HttpHeaders.AUTHORIZATION + ", " + CorrelationId.HEADER_NAME))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, DEMO_ORIGIN))
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
}
