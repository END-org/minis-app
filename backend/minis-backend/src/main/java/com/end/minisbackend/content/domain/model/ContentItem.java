package com.end.minisbackend.content.domain.model;

import com.end.minisbackend.content.domain.enums.ContentCategory;
import com.end.minisbackend.content.domain.enums.ContentStatus;
import com.end.minisbackend.content.domain.enums.ContentUploadFailureCode;
import com.end.minisbackend.content.domain.policy.ContentTextPolicy;
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
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_content_items_import_source_key",
          columnNames = "import_source_key"),
      @UniqueConstraint(
          name = "uk_content_items_creator_upload_idempotency_key",
          columnNames = {"creator_user_id", "upload_idempotency_key"})
    },
    indexes = {
      @Index(name = "idx_content_items_status_category", columnList = "status, category"),
      @Index(
          name = "idx_content_items_creator_status_published",
          columnList = "creator_user_id, status, published_at, id")
    })
public class ContentItem {
  public static final int TITLE_MAX = ContentTextPolicy.TITLE_MAX;
  public static final int DESCRIPTION_MAX = ContentTextPolicy.DESCRIPTION_MAX;
  public static final int OBJECT_KEY_MAX = 512;
  public static final int CONTENT_TYPE_MAX = 100;
  public static final int IMPORT_SOURCE_KEY_MAX = 160;
  public static final int UPLOAD_IDEMPOTENCY_KEY_MAX = 160;
  public static final int UPLOAD_FAILURE_CODE_MAX = 64;
  public static final int UPLOAD_FAILURE_REASON_MAX = 300;
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

  @Column(name = "media_content_type", length = CONTENT_TYPE_MAX)
  private String mediaContentType;

  @Column(name = "media_size_bytes")
  private Long mediaSizeBytes;

  @Column(name = "duration_ms")
  private Long durationMs;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private ContentStatus status;

  @Column(name = "import_source_key", length = IMPORT_SOURCE_KEY_MAX)
  private String importSourceKey;

  @Column(nullable = false)
  private Instant createdAt;

  @Column private Instant publishedAt;

  @Column(name = "removed_at")
  private Instant removedAt;

  @Column(name = "removal_reason", length = REMOVAL_REASON_MAX)
  private String removalReason;

  @Column(name = "upload_idempotency_key", length = UPLOAD_IDEMPOTENCY_KEY_MAX)
  private String uploadIdempotencyKey;

  @Column(name = "upload_started_at")
  private Instant uploadStartedAt;

  @Column(name = "upload_completed_at")
  private Instant uploadCompletedAt;

  @Column(name = "upload_failure_code", length = UPLOAD_FAILURE_CODE_MAX)
  private String uploadFailureCode;

  @Column(name = "upload_failure_reason", length = UPLOAD_FAILURE_REASON_MAX)
  private String uploadFailureReason;

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

  public static ContentItem reserveUpload(
      UUID id,
      UUID creatorUserId,
      String title,
      String description,
      ContentCategory category,
      String mediaObjectKey,
      String uploadIdempotencyKey,
      Instant now) {
    ContentItem item = new ContentItem();
    item.id = Objects.requireNonNull(id, "id is required");
    item.creatorUserId = Objects.requireNonNull(creatorUserId, "creatorUserId is required");
    item.title = requiredBounded(title, TITLE_MAX);
    item.description = requiredBounded(description, DESCRIPTION_MAX);
    item.category = Objects.requireNonNull(category, "category is required");
    item.mediaObjectKey = requiredBounded(mediaObjectKey, OBJECT_KEY_MAX);
    item.uploadIdempotencyKey = requiredBounded(uploadIdempotencyKey, UPLOAD_IDEMPOTENCY_KEY_MAX);
    item.status = ContentStatus.UPLOADING;
    item.createdAt = Objects.requireNonNull(now, "now is required");
    item.uploadStartedAt = now;
    return item;
  }

  public void publishUpload(ValidatedMediaMetadata media, Instant now) {
    if (status != ContentStatus.UPLOADING) {
      throw new IllegalStateException("Only UPLOADING content can be published");
    }
    this.mediaContentType = requiredBounded(media.contentType(), CONTENT_TYPE_MAX);
    this.mediaSizeBytes = positive(media.sizeBytes(), "mediaSizeBytes");
    this.durationMs = positiveOrNull(media.durationMs(), "durationMs");
    this.status = ContentStatus.PUBLISHED;
    this.publishedAt = Objects.requireNonNull(now, "now is required");
    this.uploadCompletedAt = now;
    this.uploadFailureCode = null;
    this.uploadFailureReason = null;
  }

  public boolean failUpload(
      ContentUploadFailureCode failureCode, ValidatedMediaMetadata media, Instant now) {
    if (status != ContentStatus.UPLOADING) {
      return false;
    }
    if (media != null) {
      this.mediaContentType = requiredBounded(media.contentType(), CONTENT_TYPE_MAX);
      this.mediaSizeBytes = positive(media.sizeBytes(), "mediaSizeBytes");
      this.durationMs = positiveOrNull(media.durationMs(), "durationMs");
    }
    this.status = ContentStatus.FAILED;
    this.uploadCompletedAt = Objects.requireNonNull(now, "now is required");
    this.uploadFailureCode = requiredBounded(failureCode.name(), UPLOAD_FAILURE_CODE_MAX);
    this.uploadFailureReason =
        requiredBounded(failureCode.defaultReason(), UPLOAD_FAILURE_REASON_MAX);
    return true;
  }

  public boolean remove(String reason, Instant now) {
    if (status == ContentStatus.REMOVED) {
      return false;
    }
    if (status != ContentStatus.PUBLISHED) {
      throw new IllegalStateException("Only PUBLISHED content can be removed");
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

  public boolean matchesUploadMetadata(
      String expectedTitle, String expectedDescription, ContentCategory expectedCategory) {
    return title.equals(requiredBounded(expectedTitle, TITLE_MAX))
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

  public Long mediaSizeBytes() {
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

  public String uploadIdempotencyKey() {
    return uploadIdempotencyKey;
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

  public Instant uploadStartedAt() {
    return uploadStartedAt;
  }

  public Instant uploadCompletedAt() {
    return uploadCompletedAt;
  }

  public String uploadFailureCode() {
    return uploadFailureCode;
  }

  public String uploadFailureReason() {
    return uploadFailureReason;
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
