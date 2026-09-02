package com.end.minisbackend.content.api;

import com.end.minisbackend.content.application.ContentUploadQueryService;
import com.end.minisbackend.content.application.ContentUploadService;
import com.end.minisbackend.content.application.ContentUploadSubmission;
import com.end.minisbackend.content.application.UploadContentCommand;
import com.end.minisbackend.identity.application.MinisPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
class ContentUploadController {
  static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

  private final ContentUploadService contentUploadService;
  private final ContentUploadQueryService contentUploadQueryService;

  ContentUploadController(
      ContentUploadService contentUploadService,
      ContentUploadQueryService contentUploadQueryService) {
    this.contentUploadService = contentUploadService;
    this.contentUploadQueryService = contentUploadQueryService;
  }

  @Operation(
      summary = "Upload a creator video",
      security = @SecurityRequirement(name = "firebase-id-token"),
      parameters =
          @Parameter(
              name = IDEMPOTENCY_KEY_HEADER,
              in = ParameterIn.HEADER,
              required = true,
              description = "Stable key for one logical upload attempt."))
  @RequestBody(
      required = true,
      content =
          @Content(
              mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
              schema = @Schema(implementation = UploadContentMultipartRequest.class),
              encoding = {
                @Encoding(name = "metadata", contentType = MediaType.APPLICATION_JSON_VALUE),
                @Encoding(name = "video", contentType = EventMediaProfile.CONTENT_TYPE)
              }))
  @PostMapping(value = "/content", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ResponseEntity<ContentUploadResponse> upload(
      @AuthenticationPrincipal MinisPrincipal principal,
      @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
      @Valid @RequestPart("metadata") UploadContentMetadataRequest metadata,
      @RequestPart("video") MultipartFile video) {
    ContentUploadSubmission submission =
        contentUploadService.upload(
            principal,
            new UploadContentCommand(
                idempotencyKey,
                metadata.title(),
                metadata.description(),
                metadata.category(),
                new MultipartUploadVideoSource(video)));
    ContentUploadResponse response = ContentUploadResponseMapper.toResponse(submission.item());
    if (!submission.created()) {
      return ResponseEntity.ok(response);
    }
    return ResponseEntity.created(URI.create("/api/v1/content/" + response.id())).body(response);
  }

  @Operation(
      summary = "List current creator uploads",
      security = @SecurityRequirement(name = "firebase-id-token"))
  @GetMapping("/me/uploads")
  ContentUploadPageResponse uploads(
      @AuthenticationPrincipal MinisPrincipal principal,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) Integer size) {
    return ContentUploadResponseMapper.toResponse(
        contentUploadQueryService.uploads(principal, cursor, size));
  }
}
