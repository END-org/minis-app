package com.end.minisbackend.content.infrastructure;

import com.end.minisbackend.content.domain.ContentItem;
import com.end.minisbackend.content.domain.ContentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface ContentItemRepository extends JpaRepository<ContentItem, UUID> {
  Optional<ContentItem> findByImportSourceKey(String importSourceKey);

  List<ContentItem> findByStatusOrderByIdAsc(ContentStatus status, Pageable pageable);

  List<ContentItem> findByStatusAndIdGreaterThanOrderByIdAsc(
      ContentStatus status, UUID afterContentId, Pageable pageable);
}
