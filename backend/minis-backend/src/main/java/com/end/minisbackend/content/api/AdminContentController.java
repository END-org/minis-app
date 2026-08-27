package com.end.minisbackend.content.api;

import com.end.minisbackend.content.application.ContentAdministrationService;
import com.end.minisbackend.identity.application.MinisPrincipal;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/content")
class AdminContentController {
  private final ContentAdministrationService contentAdministrationService;

  AdminContentController(ContentAdministrationService contentAdministrationService) {
    this.contentAdministrationService = contentAdministrationService;
  }

  @PutMapping("/{contentId}/removal")
  ResponseEntity<Void> remove(
      @AuthenticationPrincipal MinisPrincipal principal,
      @PathVariable UUID contentId,
      @Valid @RequestBody RemoveContentRequest request) {
    contentAdministrationService.remove(principal, contentId, request.reason());
    return ResponseEntity.noContent().build();
  }
}
