package com.end.minisbackend.creator.api;

import com.end.minisbackend.creator.application.CreatorProfileQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/creators")
class CreatorController {
  private final CreatorProfileQueryService creatorProfileQueryService;

  CreatorController(CreatorProfileQueryService creatorProfileQueryService) {
    this.creatorProfileQueryService = creatorProfileQueryService;
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
}
