package com.end.minisbackend.identity.api.controller;

import com.end.minisbackend.identity.api.dto.AuthBootstrapResponse;
import com.end.minisbackend.identity.api.mapper.AuthBootstrapResponseMapper;
import com.end.minisbackend.identity.application.model.MinisPrincipal;
import com.end.minisbackend.identity.application.model.ProvisionedUserAccount;
import com.end.minisbackend.identity.application.port.BootstrapAuthenticationContextRefresher;
import com.end.minisbackend.identity.application.service.BootstrapCurrentIdentityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
class AuthBootstrapController {
  private final BootstrapCurrentIdentityService bootstrapCurrentIdentityService;
  private final BootstrapAuthenticationContextRefresher authenticationContextRefresher;

  AuthBootstrapController(
      BootstrapCurrentIdentityService bootstrapCurrentIdentityService,
      BootstrapAuthenticationContextRefresher authenticationContextRefresher) {
    this.bootstrapCurrentIdentityService = bootstrapCurrentIdentityService;
    this.authenticationContextRefresher = authenticationContextRefresher;
  }

  @Operation(
      summary = "Bootstrap current MINIS account",
      description =
          "Creates or loads the MINIS user account attached to the verified Firebase ID token.",
      security = @SecurityRequirement(name = "firebase-id-token"))
  @ApiResponse(responseCode = "200", description = "The provisioned MINIS user account.")
  @ApiResponse(responseCode = "401", description = "The Firebase ID token is missing or invalid.")
  @ApiResponse(responseCode = "403", description = "Email is unverified or account is not active.")
  @PostMapping("/bootstrap")
  ResponseEntity<AuthBootstrapResponse> bootstrap(
      @AuthenticationPrincipal MinisPrincipal principal) {
    ProvisionedUserAccount account =
        bootstrapCurrentIdentityService.bootstrap(principal.firebaseIdentity());
    authenticationContextRefresher.refresh(principal.firebaseIdentity(), account);
    return ResponseEntity.ok(AuthBootstrapResponseMapper.toResponse(account));
  }
}
