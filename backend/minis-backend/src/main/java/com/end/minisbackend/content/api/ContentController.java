package com.end.minisbackend.content.api;

import com.end.minisbackend.content.application.PublicContentQueryService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class ContentController {
  private final PublicContentQueryService publicContentQueryService;

  ContentController(PublicContentQueryService publicContentQueryService) {
    this.publicContentQueryService = publicContentQueryService;
  }

  @Operation(summary = "Get public latest content feed")
  @GetMapping("/feed")
  ContentPageResponse feed(
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) Integer limit,
      @RequestParam(required = false) String category) {
    return ContentResponseMapper.toResponse(
        publicContentQueryService.feed(cursor, limit, category));
  }

  @Operation(summary = "Get public content item")
  @GetMapping("/content/{contentId}")
  ContentResponse content(@PathVariable UUID contentId) {
    return ContentResponseMapper.toResponse(publicContentQueryService.content(contentId));
  }

  @Operation(summary = "Get public content for a creator")
  @GetMapping("/creators/{slug}/content")
  ContentPageResponse creatorContent(
      @PathVariable String slug,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) Integer limit) {
    return ContentResponseMapper.toResponse(
        publicContentQueryService.creatorContent(slug, cursor, limit));
  }
}
