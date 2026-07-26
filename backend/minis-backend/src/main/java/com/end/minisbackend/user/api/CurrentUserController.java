package com.end.minisbackend.user.api;

import com.end.minisbackend.identity.application.MinisPrincipal;
import com.end.minisbackend.user.application.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class CurrentUserController {
  private final CurrentUserService currentUserService;

  CurrentUserController(CurrentUserService currentUserService) {
    this.currentUserService = currentUserService;
  }

  @Operation(
      summary = "Get current MINIS user",
      description =
          "Returns the provisioned active MINIS account for the Firebase-authenticated caller.",
      security = @SecurityRequirement(name = "firebase-id-token"))
  @ApiResponse(responseCode = "200", description = "The current MINIS user account.")
  @ApiResponse(responseCode = "401", description = "The Firebase ID token is missing or invalid.")
  @ApiResponse(responseCode = "403", description = "The MINIS account is missing or inactive.")
  @GetMapping("/me")
  ResponseEntity<CurrentUserResponse> me(@AuthenticationPrincipal MinisPrincipal principal) {
    return ResponseEntity.ok(
        CurrentUserResponseMapper.toResponse(currentUserService.currentUser(principal)));
  }
}
