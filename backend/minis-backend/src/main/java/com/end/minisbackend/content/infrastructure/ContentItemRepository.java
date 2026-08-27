package com.end.minisbackend.content.infrastructure;

import com.end.minisbackend.content.domain.ContentItem;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ContentItemRepository extends JpaRepository<ContentItem, UUID> {
  Optional<ContentItem> findByImportSourceKey(String importSourceKey);
}
