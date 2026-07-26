package com.end.minisbackend.identity.api;

import com.end.minisbackend.identity.application.BootstrapCurrentIdentityService;
import com.end.minisbackend.identity.application.MinisPrincipal;
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

  AuthBootstrapController(BootstrapCurrentIdentityService bootstrapCurrentIdentityService) {
    this.bootstrapCurrentIdentityService = bootstrapCurrentIdentityService;
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
    return ResponseEntity.ok(
        AuthBootstrapResponseMapper.toResponse(
            bootstrapCurrentIdentityService.bootstrap(principal.firebaseIdentity())));
  }
}
