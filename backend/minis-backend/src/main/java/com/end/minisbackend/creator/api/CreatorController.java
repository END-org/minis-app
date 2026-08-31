package com.end.minisbackend.creator.api;

import com.end.minisbackend.creator.application.CreatorFollowService;
import com.end.minisbackend.creator.application.CreatorProfileQueryService;
import com.end.minisbackend.identity.application.MinisPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/creators")
class CreatorController {
  private final CreatorProfileQueryService creatorProfileQueryService;
  private final CreatorFollowService creatorFollowService;

  CreatorController(
      CreatorProfileQueryService creatorProfileQueryService,
      CreatorFollowService creatorFollowService) {
    this.creatorProfileQueryService = creatorProfileQueryService;
    this.creatorFollowService = creatorFollowService;
  }

  @Operation(summary = "List public active creators")
  @ApiResponse(responseCode = "200", description = "Public active creator directory.")
  @GetMapping
  ResponseEntity<CreatorDirectoryResponse> creators(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
    return ResponseEntity.ok(
        CreatorResponseMapper.toResponse(creatorProfileQueryService.publicCreators(page, size)));
  }

  @Operation(summary = "Get a public active creator by slug")
  @ApiResponse(responseCode = "200", description = "Public active creator profile.")
  @ApiResponse(responseCode = "404", description = "No public active creator has this slug.")
  @GetMapping("/{slug}")
  ResponseEntity<CreatorResponse> creator(@PathVariable String slug) {
    return ResponseEntity.ok(
        CreatorResponseMapper.toResponse(creatorProfileQueryService.publicCreator(slug)));
  }

  @Operation(
      summary = "Ensure the current user follows a public creator",
      security = @SecurityRequirement(name = "firebase-id-token"))
  @PutMapping("/{slug}/follow")
  ResponseEntity<Void> follow(
      @AuthenticationPrincipal MinisPrincipal principal, @PathVariable String slug) {
    creatorFollowService.follow(principal, slug);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Ensure the current user does not follow a creator",
      security = @SecurityRequirement(name = "firebase-id-token"))
  @DeleteMapping("/{slug}/follow")
  ResponseEntity<Void> unfollow(
      @AuthenticationPrincipal MinisPrincipal principal, @PathVariable String slug) {
    creatorFollowService.unfollow(principal, slug);
    return ResponseEntity.noContent().build();
  }
}
