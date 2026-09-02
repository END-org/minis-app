package com.end.minisbackend.content.infrastructure.repository;

import com.end.minisbackend.content.domain.enums.ContentStatus;
import com.end.minisbackend.content.domain.model.ContentItem;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface ContentItemRepository extends JpaRepository<ContentItem, UUID> {
  Optional<ContentItem> findByImportSourceKey(String importSourceKey);

  Optional<ContentItem> findByCreatorUserIdAndUploadIdempotencyKey(
      UUID creatorUserId, String uploadIdempotencyKey);

  List<ContentItem> findByStatusOrderByIdAsc(ContentStatus status, Pageable pageable);

  List<ContentItem> findByStatusAndIdGreaterThanOrderByIdAsc(
      ContentStatus status, UUID afterContentId, Pageable pageable);

  List<ContentItem> findByStatusAndUploadStartedAtBeforeOrderByUploadStartedAtAscIdAsc(
      ContentStatus status, Instant uploadStartedBefore, Pageable pageable);
}
