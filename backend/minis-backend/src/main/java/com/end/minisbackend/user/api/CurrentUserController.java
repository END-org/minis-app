package com.end.minisbackend.user.api;

import com.end.minisbackend.identity.application.MinisPrincipal;
import com.end.minisbackend.user.application.CurrentUserService;
import com.end.minisbackend.user.application.UpdateCurrentProfileCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

  @Operation(
      summary = "Update current MINIS profile",
      description = "Updates only the caller-owned display name, avatar URL, and bio fields.",
      security = @SecurityRequirement(name = "firebase-id-token"))
  @ApiResponse(responseCode = "200", description = "The updated current MINIS user account.")
  @ApiResponse(responseCode = "400", description = "The profile request is invalid.")
  @ApiResponse(responseCode = "401", description = "The Firebase ID token is missing or invalid.")
  @ApiResponse(responseCode = "403", description = "The MINIS account is missing or inactive.")
  @PatchMapping("/me/profile")
  ResponseEntity<CurrentUserResponse> updateProfile(
      @AuthenticationPrincipal MinisPrincipal principal,
      @Valid @RequestBody UpdateCurrentProfileRequest request) {
    UpdateCurrentProfileCommand command =
        new UpdateCurrentProfileCommand(request.displayName(), request.avatarUrl(), request.bio());
    return ResponseEntity.ok(
        CurrentUserResponseMapper.toResponse(currentUserService.updateProfile(principal, command)));
  }
}
