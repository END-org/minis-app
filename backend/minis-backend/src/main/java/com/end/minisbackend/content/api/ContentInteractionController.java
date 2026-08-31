package com.end.minisbackend.content.api;

import com.end.minisbackend.content.application.ContentInteractionService;
import com.end.minisbackend.content.application.PublicContentQueryService;
import com.end.minisbackend.identity.application.MinisPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class ContentInteractionController {
  private final ContentInteractionService contentInteractionService;
  private final PublicContentQueryService publicContentQueryService;

  ContentInteractionController(
      ContentInteractionService contentInteractionService,
      PublicContentQueryService publicContentQueryService) {
    this.contentInteractionService = contentInteractionService;
    this.publicContentQueryService = publicContentQueryService;
  }

  @Operation(
      summary = "Ensure the current user likes published content",
      security = @SecurityRequirement(name = "firebase-id-token"))
  @PutMapping("/content/{contentId}/like")
  ResponseEntity<Void> like(
      @AuthenticationPrincipal MinisPrincipal principal, @PathVariable UUID contentId) {
    contentInteractionService.like(principal, contentId);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Ensure the current user does not like content",
      security = @SecurityRequirement(name = "firebase-id-token"))
  @DeleteMapping("/content/{contentId}/like")
  ResponseEntity<Void> unlike(
      @AuthenticationPrincipal MinisPrincipal principal, @PathVariable UUID contentId) {
    contentInteractionService.unlike(principal, contentId);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Ensure the current user privately saves published content",
      security = @SecurityRequirement(name = "firebase-id-token"))
  @PutMapping("/content/{contentId}/save")
  ResponseEntity<Void> save(
      @AuthenticationPrincipal MinisPrincipal principal, @PathVariable UUID contentId) {
    contentInteractionService.save(principal, contentId);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Ensure the current user does not have content saved",
      security = @SecurityRequirement(name = "firebase-id-token"))
  @DeleteMapping("/content/{contentId}/save")
  ResponseEntity<Void> unsave(
      @AuthenticationPrincipal MinisPrincipal principal, @PathVariable UUID contentId) {
    contentInteractionService.unsave(principal, contentId);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "List content liked by the current user",
      security = @SecurityRequirement(name = "firebase-id-token"))
  @GetMapping("/me/liked-content")
  ContentPageResponse likedContent(
      @AuthenticationPrincipal MinisPrincipal principal,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) Integer size) {
    return ContentResponseMapper.toResponse(
        publicContentQueryService.likedContent(principal, cursor, size));
  }

  @Operation(
      summary = "List content saved by the current user",
      security = @SecurityRequirement(name = "firebase-id-token"))
  @GetMapping("/me/saved-content")
  ContentPageResponse savedContent(
      @AuthenticationPrincipal MinisPrincipal principal,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) Integer size) {
    return ContentResponseMapper.toResponse(
        publicContentQueryService.savedContent(principal, cursor, size));
  }
}
