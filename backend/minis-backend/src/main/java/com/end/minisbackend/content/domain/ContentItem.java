package com.end.minisbackend.content.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.util.StringUtils;

@Entity
@Table(
    name = "content_items",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_content_items_import_source_key",
            columnNames = "import_source_key"),
    indexes = {
      @Index(name = "idx_content_items_status_category", columnList = "status, category"),
      @Index(
          name = "idx_content_items_creator_status_published",
          columnList = "creator_user_id, status, published_at, id")
    })
public class ContentItem {
  public static final int TITLE_MAX = 140;
  public static final int DESCRIPTION_MAX = 1000;
  public static final int OBJECT_KEY_MAX = 512;
  public static final int CONTENT_TYPE_MAX = 100;
  public static final int IMPORT_SOURCE_KEY_MAX = 160;
  public static final int REMOVAL_REASON_MAX = 300;

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(name = "creator_user_id", nullable = false, updatable = false)
  private UUID creatorUserId;

  @Column(nullable = false, length = TITLE_MAX)
  private String title;

  @Column(nullable = false, length = DESCRIPTION_MAX)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 64)
  private ContentCategory category;

  @Column(name = "media_object_key", nullable = false, length = OBJECT_KEY_MAX)
  private String mediaObjectKey;

  @Column(name = "media_content_type", nullable = false, length = CONTENT_TYPE_MAX)
  private String mediaContentType;

  @Column(name = "media_size_bytes", nullable = false)
  private long mediaSizeBytes;

  @Column(name = "duration_ms")
  private Long durationMs;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private ContentStatus status;

  @Column(name = "import_source_key", nullable = false, length = IMPORT_SOURCE_KEY_MAX)
  private String importSourceKey;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant publishedAt;

  @Column(name = "removed_at")
  private Instant removedAt;

  @Column(name = "removal_reason", length = REMOVAL_REASON_MAX)
  private String removalReason;

  protected ContentItem() {}

  private ContentItem(
      UUID id,
      UUID creatorUserId,
      String title,
      String description,
      ContentCategory category,
      String mediaObjectKey,
      String mediaContentType,
      long mediaSizeBytes,
      Long durationMs,
      String importSourceKey,
      Instant now) {
    this.id = id;
    this.creatorUserId = creatorUserId;
    this.title = title;
    this.description = description;
    this.category = category;
    this.mediaObjectKey = mediaObjectKey;
    this.mediaContentType = mediaContentType;
    this.mediaSizeBytes = mediaSizeBytes;
    this.durationMs = durationMs;
    this.importSourceKey = importSourceKey;
    this.status = ContentStatus.PUBLISHED;
    this.createdAt = now;
    this.publishedAt = now;
  }

  public static ContentItem publishImported(
      UUID creatorUserId,
      String title,
      String description,
      ContentCategory category,
      String mediaObjectKey,
      String mediaContentType,
      long mediaSizeBytes,
      Long durationMs,
      String importSourceKey,
      Instant now) {
    return new ContentItem(
        UUID.randomUUID(),
        Objects.requireNonNull(creatorUserId, "creatorUserId is required"),
        requiredBounded(title, TITLE_MAX),
        requiredBounded(description, DESCRIPTION_MAX),
        Objects.requireNonNull(category, "category is required"),
        requiredBounded(mediaObjectKey, OBJECT_KEY_MAX),
        requiredBounded(mediaContentType, CONTENT_TYPE_MAX),
        positive(mediaSizeBytes, "mediaSizeBytes"),
        positiveOrNull(durationMs, "durationMs"),
        requiredBounded(importSourceKey, IMPORT_SOURCE_KEY_MAX),
        Objects.requireNonNull(now, "now is required"));
  }

  public boolean remove(String reason, Instant now) {
    if (status == ContentStatus.REMOVED) {
      return false;
    }
    this.status = ContentStatus.REMOVED;
    this.removedAt = Objects.requireNonNull(now, "now is required");
    this.removalReason = requiredBounded(reason, REMOVAL_REASON_MAX);
    return true;
  }

  public boolean matchesImportMetadata(
      UUID expectedCreatorUserId,
      String expectedTitle,
      String expectedDescription,
      ContentCategory expectedCategory) {
    return creatorUserId.equals(expectedCreatorUserId)
        && title.equals(requiredBounded(expectedTitle, TITLE_MAX))
        && description.equals(requiredBounded(expectedDescription, DESCRIPTION_MAX))
        && category == expectedCategory;
  }

  public UUID id() {
    return id;
  }

  public UUID creatorUserId() {
    return creatorUserId;
  }

  public String title() {
    return title;
  }

  public String description() {
    return description;
  }

  public ContentCategory category() {
    return category;
  }

  public String mediaObjectKey() {
    return mediaObjectKey;
  }

  public String mediaContentType() {
    return mediaContentType;
  }

  public long mediaSizeBytes() {
    return mediaSizeBytes;
  }

  public Long durationMs() {
    return durationMs;
  }

  public ContentStatus status() {
    return status;
  }

  public String importSourceKey() {
    return importSourceKey;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant publishedAt() {
    return publishedAt;
  }

  public Instant removedAt() {
    return removedAt;
  }

  public String removalReason() {
    return removalReason;
  }

  private static String requiredBounded(String value, int maxLength) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalArgumentException("value is required");
    }
    String trimmed = value.trim();
    if (trimmed.length() <= maxLength) {
      return trimmed;
    }
    throw new IllegalArgumentException("value exceeds maximum length");
  }

  private static long positive(long value, String fieldName) {
    if (value <= 0) {
      throw new IllegalArgumentException(fieldName + " must be positive");
    }
    return value;
  }

  private static Long positiveOrNull(Long value, String fieldName) {
    if (value == null) {
      return null;
    }
    if (value <= 0) {
      throw new IllegalArgumentException(fieldName + " must be positive");
    }
    return value;
  }
}
