package com.end.minisbackend.content.application.service.upload;

import com.end.minisbackend.content.application.model.importing.ContentImportException;
import com.end.minisbackend.content.application.model.upload.ContentUploadSubmission;
import com.end.minisbackend.content.application.model.upload.UploadContentCommand;
import com.end.minisbackend.content.application.port.ContentStorage;
import com.end.minisbackend.content.application.port.ContentStorageException;
import com.end.minisbackend.content.application.port.ContentUploadStore;
import com.end.minisbackend.content.application.port.UploadVideoSource;
import com.end.minisbackend.content.domain.enums.ContentCategory;
import com.end.minisbackend.content.domain.enums.ContentStatus;
import com.end.minisbackend.content.domain.enums.ContentUploadFailureCode;
import com.end.minisbackend.content.domain.model.ContentItem;
import com.end.minisbackend.content.domain.model.ValidatedMediaMetadata;
import com.end.minisbackend.content.domain.model.media.MediaValidationResult;
import com.end.minisbackend.creator.application.port.CreatorProfileStore;
import com.end.minisbackend.identity.application.model.MinisPrincipal;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class ContentUploadService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ContentUploadService.class);
  private static final Pattern IDEMPOTENCY_KEY_PATTERN = Pattern.compile("[A-Za-z0-9._:-]{8,160}");

  private final ContentUploadStore contentUploadStore;
  private final CreatorProfileStore creatorProfileStore;
  private final ContentStorage contentStorage;
  private final UploadMediaValidator uploadMediaValidator;
  private final ContentUploadQueryService uploadQueryService;
  private final PlatformTransactionManager transactionManager;
  private final Clock clock;

  public ContentUploadService(
      ContentUploadStore contentUploadStore,
      CreatorProfileStore creatorProfileStore,
      ContentStorage contentStorage,
      UploadMediaValidator uploadMediaValidator,
      ContentUploadQueryService uploadQueryService,
      PlatformTransactionManager transactionManager,
      Clock clock) {
    this.contentUploadStore = contentUploadStore;
    this.creatorProfileStore = creatorProfileStore;
    this.contentStorage = contentStorage;
    this.uploadMediaValidator = uploadMediaValidator;
    this.uploadQueryService = uploadQueryService;
    this.transactionManager = transactionManager;
    this.clock = clock;
  }

  public ContentUploadSubmission upload(MinisPrincipal principal, UploadContentCommand command) {
    UUID creatorUserId = ContentUploadQueryService.requireActiveCreator(principal);
    String idempotencyKey = normalizeIdempotencyKey(command.idempotencyKey());
    String title = requiredBounded(command.title(), ContentItem.TITLE_MAX);
    String description = requiredBounded(command.description(), ContentItem.DESCRIPTION_MAX);
    ContentCategory category = ContentCategory.parse(command.category());
    ContentUploadReservation reservation =
        reserveUpload(creatorUserId, idempotencyKey, title, description, category);
    ContentItem content = reservation.content();
    if (!content.matchesUploadMetadata(title, description, category)) {
      throw new ApiException(ApiErrorCode.RESOURCE_CONFLICT);
    }
    if (!reservation.created()) {
      return existingUpload(principal, content);
    }
    LOGGER.info(
        "event=upload_started localUserId={} contentId={} uploadStatus={}",
        creatorUserId,
        content.id(),
        content.status());
    return executeNewUpload(principal, command.video(), content);
  }

  private ContentUploadSubmission existingUpload(MinisPrincipal principal, ContentItem content) {
    if (content.status() == ContentStatus.UPLOADING) {
      throw new ApiException(ApiErrorCode.UPLOAD_IN_PROGRESS);
    }
    if (content.status() == ContentStatus.FAILED) {
      throw new ApiException(ApiErrorCode.UPLOAD_ALREADY_FAILED);
    }
    return new ContentUploadSubmission(
        uploadQueryService.uploadForOwner(principal, content.id()), false);
  }

  private ContentUploadSubmission executeNewUpload(
      MinisPrincipal principal, UploadVideoSource video, ContentItem content) {
    Path tempFile = null;
    MediaValidationResult media = null;
    try {
      tempFile = uploadMediaValidator.stage(video);
      media = uploadMediaValidator.inspect(tempFile);
      contentStorage.ensureBucket();
      contentStorage.uploadObject(
          content.mediaObjectKey(), tempFile, media.contentType(), media.sizeBytes());
      LOGGER.info("event=upload_storage_completed contentId={}", content.id());
      publish(content.id(), toDomainMedia(media));
      LOGGER.info("event=upload_published contentId={}", content.id());
      return new ContentUploadSubmission(
          uploadQueryService.uploadForOwner(principal, content.id()), true);
    } catch (ContentUploadRejectedException exception) {
      failUpload(content.id(), exception.failureCode(), null);
      LOGGER.info(
          "event=upload_validation_failed contentId={} failureCode={}",
          content.id(),
          exception.failureCode());
      throw new ApiException(exception.apiErrorCode(), exception);
    } catch (ContentImportException exception) {
      failUpload(content.id(), ContentUploadFailureCode.UNSUPPORTED_MEDIA_PROFILE, null);
      LOGGER.info(
          "event=upload_validation_failed contentId={} failureCode={}",
          content.id(),
          ContentUploadFailureCode.UNSUPPORTED_MEDIA_PROFILE);
      throw new ApiException(ApiErrorCode.UPLOAD_VALIDATION_FAILED, exception);
    } catch (ContentStorageException exception) {
      compensateUploadedObject(content.id(), content.mediaObjectKey());
      failUpload(
          content.id(),
          ContentUploadFailureCode.STORAGE_UPLOAD_FAILED,
          media == null ? null : toDomainMedia(media));
      LOGGER.warn(
          "event=upload_failed contentId={} failureCode={}",
          content.id(),
          ContentUploadFailureCode.STORAGE_UPLOAD_FAILED);
      throw new ApiException(ApiErrorCode.INTERNAL_ERROR, exception);
    } catch (DataAccessException exception) {
      compensateUploadedObject(content.id(), content.mediaObjectKey());
      failUpload(
          content.id(),
          ContentUploadFailureCode.PUBLICATION_FAILED,
          media == null ? null : toDomainMedia(media));
      LOGGER.warn(
          "event=upload_failed contentId={} failureCode={}",
          content.id(),
          ContentUploadFailureCode.PUBLICATION_FAILED);
      throw new ApiException(ApiErrorCode.INTERNAL_ERROR, exception);
    } catch (IOException exception) {
      failUpload(content.id(), ContentUploadFailureCode.TEMPORARY_FILE_FAILED, null);
      LOGGER.warn(
          "event=upload_failed contentId={} failureCode={}",
          content.id(),
          ContentUploadFailureCode.TEMPORARY_FILE_FAILED);
      throw new ApiException(ApiErrorCode.INTERNAL_ERROR, exception);
    } finally {
      deleteTemporaryFile(tempFile);
    }
  }

  private ContentUploadReservation reserveUpload(
      UUID creatorUserId,
      String idempotencyKey,
      String title,
      String description,
      ContentCategory category) {
    ContentUploadReservation existing =
        readOnlyTransaction()
            .execute(
                status ->
                    contentUploadStore
                        .findByCreatorUserIdAndUploadIdempotencyKey(creatorUserId, idempotencyKey)
                        .map(content -> new ContentUploadReservation(content, false))
                        .orElse(null));
    if (existing != null) {
      return existing;
    }
    UUID contentId = UUID.randomUUID();
    String objectKey = "content/uploads/" + contentId + ".mp4";
    try {
      return writeTransaction()
          .execute(
              status -> {
                if (creatorProfileStore.findPublicByUserId(creatorUserId).isEmpty()) {
                  throw new ApiException(ApiErrorCode.ACCESS_DENIED);
                }
                ContentItem content =
                    ContentItem.reserveUpload(
                        contentId,
                        creatorUserId,
                        title,
                        description,
                        category,
                        objectKey,
                        idempotencyKey,
                        Instant.now(clock));
                return new ContentUploadReservation(contentUploadStore.saveAndFlush(content), true);
              });
    } catch (DataIntegrityViolationException exception) {
      return readOnlyTransaction()
          .execute(
              status ->
                  contentUploadStore
                      .findByCreatorUserIdAndUploadIdempotencyKey(creatorUserId, idempotencyKey)
                      .map(content -> new ContentUploadReservation(content, false))
                      .orElseThrow(() -> exception));
    }
  }

  private void publish(UUID contentId, ValidatedMediaMetadata media) {
    writeTransaction()
        .execute(
            status -> {
              ContentItem content =
                  contentUploadStore
                      .findById(contentId)
                      .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND));
              content.publishUpload(media, Instant.now(clock));
              contentUploadStore.saveAndFlush(content);
              return null;
            });
  }

  private void failUpload(
      UUID contentId, ContentUploadFailureCode failureCode, ValidatedMediaMetadata media) {
    try {
      writeTransaction()
          .execute(
              status -> {
                contentUploadStore
                    .findById(contentId)
                    .ifPresent(
                        content -> {
                          if (content.failUpload(failureCode, media, Instant.now(clock))) {
                            contentUploadStore.saveAndFlush(content);
                          }
                        });
                return null;
              });
    } catch (DataAccessException exception) {
      LOGGER.warn(
          "event=upload_failure_state_update_failed contentId={} failureCode={} exceptionCategory={}",
          contentId,
          failureCode,
          exception.getClass().getSimpleName());
    }
  }

  private void compensateUploadedObject(UUID contentId, String objectKey) {
    try {
      if (contentUploadStore.publishedContentOwnsObject(contentId, objectKey)) {
        LOGGER.warn("event=upload_compensation_skipped_existing_owner contentId={}", contentId);
        return;
      }
    } catch (DataAccessException exception) {
      LOGGER.error(
          "event=upload_orphan_candidate contentId={} exceptionCategory={}",
          contentId,
          exception.getClass().getSimpleName());
      return;
    }
    try {
      contentStorage.deleteNewlyUploadedObject(objectKey);
      LOGGER.warn("event=upload_compensation_completed contentId={}", contentId);
    } catch (ContentStorageException exception) {
      LOGGER.error(
          "event=upload_orphan_candidate contentId={} exceptionCategory={}",
          contentId,
          exception.getClass().getSimpleName());
    }
  }

  private static void deleteTemporaryFile(Path tempFile) {
    if (tempFile == null) {
      return;
    }
    try {
      Files.deleteIfExists(tempFile);
    } catch (IOException exception) {
      LOGGER.warn(
          "event=upload_temp_cleanup_failed exceptionCategory={}",
          exception.getClass().getSimpleName());
    }
  }

  private static String normalizeIdempotencyKey(String idempotencyKey) {
    String trimmed = idempotencyKey == null ? "" : idempotencyKey.trim();
    if (!IDEMPOTENCY_KEY_PATTERN.matcher(trimmed).matches()) {
      throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
    }
    return trimmed;
  }

  private static String requiredBounded(String value, int maxLength) {
    if (!StringUtils.hasText(value)) {
      throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
    }
    String trimmed = value.trim();
    if (trimmed.length() > maxLength) {
      throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
    }
    return trimmed;
  }

  private static ValidatedMediaMetadata toDomainMedia(MediaValidationResult media) {
    return new ValidatedMediaMetadata(media.contentType(), media.sizeBytes(), media.durationMs());
  }

  private TransactionTemplate readOnlyTransaction() {
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);
    return transaction;
  }

  private TransactionTemplate writeTransaction() {
    return new TransactionTemplate(transactionManager);
  }
}
