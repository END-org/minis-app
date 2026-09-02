package com.end.minisbackend.admin.api.controller;

import com.end.minisbackend.admin.api.dto.AdminReasonRequest;
import com.end.minisbackend.admin.api.dto.AdminUserListResponse;
import com.end.minisbackend.admin.api.dto.AdminUserResponse;
import com.end.minisbackend.admin.api.dto.GrantCreatorRoleRequest;
import com.end.minisbackend.admin.api.mapper.AdminUserResponseMapper;
import com.end.minisbackend.admin.application.model.AdminReasonCommand;
import com.end.minisbackend.admin.application.model.GrantCreatorRoleCommand;
import com.end.minisbackend.admin.application.service.AccountStatusAdministrationService;
import com.end.minisbackend.admin.application.service.AdminUserQueryService;
import com.end.minisbackend.admin.application.service.CreatorRoleAdministrationService;
import com.end.minisbackend.admin.application.service.CreatorVerificationAdministrationService;
import com.end.minisbackend.identity.application.model.MinisPrincipal;
import com.end.minisbackend.shared.security.CorrelationId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin")
class EventAdminController {
  private final AdminUserQueryService adminUserQueryService;
  private final CreatorRoleAdministrationService creatorRoleAdministrationService;
  private final AccountStatusAdministrationService accountStatusAdministrationService;
  private final CreatorVerificationAdministrationService creatorVerificationAdministrationService;

  EventAdminController(
      AdminUserQueryService adminUserQueryService,
      CreatorRoleAdministrationService creatorRoleAdministrationService,
      AccountStatusAdministrationService accountStatusAdministrationService,
      CreatorVerificationAdministrationService creatorVerificationAdministrationService) {
    this.adminUserQueryService = adminUserQueryService;
    this.creatorRoleAdministrationService = creatorRoleAdministrationService;
    this.accountStatusAdministrationService = accountStatusAdministrationService;
    this.creatorVerificationAdministrationService = creatorVerificationAdministrationService;
  }

  @Operation(
      summary = "List users for event preparation",
      security = @SecurityRequirement(name = "firebase-id-token"))
  @ApiResponse(responseCode = "200", description = "A bounded user list for event preparation.")
  @GetMapping("/users")
  ResponseEntity<AdminUserListResponse> users(
      @AuthenticationPrincipal MinisPrincipal principal,
      @RequestParam(required = false) @Size(max = 120) String query,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
    return ResponseEntity.ok(
        AdminUserResponseMapper.toResponse(
            adminUserQueryService.users(principal, query, page, size)));
  }

  @Operation(
      summary = "Get a user for event preparation",
      security = @SecurityRequirement(name = "firebase-id-token"))
  @GetMapping("/users/{userId}")
  ResponseEntity<AdminUserResponse> user(
      @AuthenticationPrincipal MinisPrincipal principal, @PathVariable UUID userId) {
    return ResponseEntity.ok(
        AdminUserResponseMapper.toResponse(adminUserQueryService.user(principal, userId)));
  }

  @Operation(
      summary = "Grant creator capability",
      security = @SecurityRequirement(name = "firebase-id-token"))
  @PutMapping("/users/{userId}/creator-role")
  ResponseEntity<AdminUserResponse> grantCreator(
      @AuthenticationPrincipal MinisPrincipal principal,
      @PathVariable UUID userId,
      @Valid @RequestBody GrantCreatorRoleRequest request,
      HttpServletRequest httpRequest) {
    GrantCreatorRoleCommand command =
        new GrantCreatorRoleCommand(
            request.slug(),
            request.displayName(),
            request.bio(),
            request.avatarUrl(),
            request.reason());
    return ResponseEntity.ok(
        AdminUserResponseMapper.toResponse(
            creatorRoleAdministrationService.grantCreator(
                principal, userId, command, CorrelationId.fromRequest(httpRequest))));
  }

  @Operation(
      summary = "Revoke creator capability",
      security = @SecurityRequirement(name = "firebase-id-token"))
  @DeleteMapping("/users/{userId}/creator-role")
  ResponseEntity<AdminUserResponse> revokeCreator(
      @AuthenticationPrincipal MinisPrincipal principal,
      @PathVariable UUID userId,
      @Valid @RequestBody(required = false) AdminReasonRequest request,
      HttpServletRequest httpRequest) {
    return ResponseEntity.ok(
        AdminUserResponseMapper.toResponse(
            creatorRoleAdministrationService.revokeCreator(
                principal,
                userId,
                reasonCommand(request),
                CorrelationId.fromRequest(httpRequest))));
  }

  @Operation(
      summary = "Suspend account",
      security = @SecurityRequirement(name = "firebase-id-token"))
  @PutMapping("/users/{userId}/suspension")
  ResponseEntity<AdminUserResponse> suspend(
      @AuthenticationPrincipal MinisPrincipal principal,
      @PathVariable UUID userId,
      @Valid @RequestBody(required = false) AdminReasonRequest request,
      HttpServletRequest httpRequest) {
    return ResponseEntity.ok(
        AdminUserResponseMapper.toResponse(
            accountStatusAdministrationService.suspend(
                principal,
                userId,
                reasonCommand(request),
                CorrelationId.fromRequest(httpRequest))));
  }

  @Operation(
      summary = "Reactivate suspended account",
      security = @SecurityRequirement(name = "firebase-id-token"))
  @DeleteMapping("/users/{userId}/suspension")
  ResponseEntity<AdminUserResponse> reactivate(
      @AuthenticationPrincipal MinisPrincipal principal,
      @PathVariable UUID userId,
      @Valid @RequestBody(required = false) AdminReasonRequest request,
      HttpServletRequest httpRequest) {
    return ResponseEntity.ok(
        AdminUserResponseMapper.toResponse(
            accountStatusAdministrationService.reactivate(
                principal,
                userId,
                reasonCommand(request),
                CorrelationId.fromRequest(httpRequest))));
  }

  @Operation(
      summary = "Verify creator identity",
      security = @SecurityRequirement(name = "firebase-id-token"))
  @PutMapping("/creators/{userId}/verification")
  ResponseEntity<AdminUserResponse> verifyCreator(
      @AuthenticationPrincipal MinisPrincipal principal,
      @PathVariable UUID userId,
      @Valid @RequestBody(required = false) AdminReasonRequest request,
      HttpServletRequest httpRequest) {
    return ResponseEntity.ok(
        AdminUserResponseMapper.toResponse(
            creatorVerificationAdministrationService.verifyCreator(
                principal,
                userId,
                reasonCommand(request),
                CorrelationId.fromRequest(httpRequest))));
  }

  @Operation(
      summary = "Remove creator verification",
      security = @SecurityRequirement(name = "firebase-id-token"))
  @DeleteMapping("/creators/{userId}/verification")
  ResponseEntity<AdminUserResponse> unverifyCreator(
      @AuthenticationPrincipal MinisPrincipal principal,
      @PathVariable UUID userId,
      @Valid @RequestBody(required = false) AdminReasonRequest request,
      HttpServletRequest httpRequest) {
    return ResponseEntity.ok(
        AdminUserResponseMapper.toResponse(
            creatorVerificationAdministrationService.unverifyCreator(
                principal,
                userId,
                reasonCommand(request),
                CorrelationId.fromRequest(httpRequest))));
  }

  private static AdminReasonCommand reasonCommand(AdminReasonRequest request) {
    return new AdminReasonCommand(request == null ? null : request.reason());
  }
}
