package com.end.minisbackend.content.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.end.minisbackend.TestIdentities;
import com.end.minisbackend.TestTransactionManager;
import com.end.minisbackend.content.domain.ContentCategory;
import com.end.minisbackend.content.domain.ContentItem;
import com.end.minisbackend.content.domain.ContentStatus;
import com.end.minisbackend.creator.application.CreatorFollowStore;
import com.end.minisbackend.creator.application.CreatorProfileQueryService;
import com.end.minisbackend.creator.application.CreatorProfileStore;
import com.end.minisbackend.identity.application.MinisPrincipal;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ContentUploadServiceTest {
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-07-18T12:00:00Z"), ZoneOffset.UTC);
  private static final UUID CREATOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID VIEWER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
  private static final TestTransactionManager TRANSACTION_MANAGER = new TestTransactionManager();

  @TempDir Path tempDir;

  @Test
  void creatorUploadPublishesContentAndSuccessfulReplayIsIdempotent() {
    RecordingContentUploadStore uploadStore = new RecordingContentUploadStore();
    RecordingContentStorage storage = new RecordingContentStorage();
    ContentUploadService service =
        service(uploadStore, storage, validProbe(), EventMediaProfilePolicy.MAX_SIZE_BYTES);
    UploadContentCommand command = validCommand("creator-upload-1");

    ContentUploadSubmission first = service.upload(creatorPrincipal(), command);
    ContentUploadSubmission replay = service.upload(creatorPrincipal(), command);

    assertThat(first.created()).isTrue();
    assertThat(replay.created()).isFalse();
    assertThat(first.item().status()).isEqualTo(ContentStatus.PUBLISHED.name());
    assertThat(replay.item().content()).isNotNull();
    assertThat(storage.uploadCount()).isEqualTo(1);
    assertThat(storage.bucketEnsured()).isTrue();
    ContentItem stored = uploadStore.byId(first.item().id()).orElseThrow();
    assertThat(stored.status()).isEqualTo(ContentStatus.PUBLISHED);
    assertThat(stored.mediaObjectKey()).isEqualTo("content/uploads/" + stored.id() + ".mp4");
    assertThat(stored.mediaContentType()).isEqualTo("video/mp4");
    assertThat(stored.uploadFailureCode()).isNull();
    assertThat(stored.importSourceKey()).isNull();
  }

  @Test
  void sameIdempotencyKeyWithDifferentMetadataIsRejectedWithoutUploadingAgain() {
    RecordingContentUploadStore uploadStore = new RecordingContentUploadStore();
    RecordingContentStorage storage = new RecordingContentStorage();
    ContentUploadService service =
        service(uploadStore, storage, validProbe(), EventMediaProfilePolicy.MAX_SIZE_BYTES);
    service.upload(creatorPrincipal(), validCommand("creator-upload-2"));

    UploadContentCommand changed =
        new UploadContentCommand(
            "creator-upload-2",
            "Different title",
            "Event description",
            "EDUCATION",
            new BytesUploadVideoSource("clip.mp4", "video/mp4", mp4Bytes()));

    assertThatThrownBy(() -> service.upload(creatorPrincipal(), changed))
        .isInstanceOf(ApiException.class)
        .satisfies(throwable -> assertApiError(throwable, ApiErrorCode.RESOURCE_CONFLICT));
    assertThat(storage.uploadCount()).isEqualTo(1);
  }

  @Test
  void nonCreatorCannotReserveUpload() {
    RecordingContentUploadStore uploadStore = new RecordingContentUploadStore();
    RecordingContentStorage storage = new RecordingContentStorage();
    ContentUploadService service =
        service(uploadStore, storage, validProbe(), EventMediaProfilePolicy.MAX_SIZE_BYTES);

    assertThatThrownBy(() -> service.upload(viewerPrincipal(), validCommand("viewer-upload-1")))
        .isInstanceOf(ApiException.class)
        .satisfies(throwable -> assertApiError(throwable, ApiErrorCode.ACCESS_DENIED));
    assertThat(uploadStore.all()).isEmpty();
    assertThat(storage.uploadCount()).isZero();
  }

  @Test
  void invalidTransportMarksReservedUploadFailed() {
    RecordingContentUploadStore uploadStore = new RecordingContentUploadStore();
    RecordingContentStorage storage = new RecordingContentStorage();
    ContentUploadService service =
        service(uploadStore, storage, validProbe(), EventMediaProfilePolicy.MAX_SIZE_BYTES);
    UploadContentCommand command =
        new UploadContentCommand(
            "invalid-type-1",
            "Event title",
            "Event description",
            "EDUCATION",
            new BytesUploadVideoSource("clip.mp4", "video/quicktime", mp4Bytes()));

    assertThatThrownBy(() -> service.upload(creatorPrincipal(), command))
        .isInstanceOf(ApiException.class)
        .satisfies(throwable -> assertApiError(throwable, ApiErrorCode.UPLOAD_VALIDATION_FAILED));

    ContentItem failed = uploadStore.all().get(0);
    assertThat(failed.status()).isEqualTo(ContentStatus.FAILED);
    assertThat(failed.uploadFailureCode()).isEqualTo("UNSUPPORTED_CONTENT_TYPE");
    assertThat(failed.uploadFailureReason()).doesNotContain("video/quicktime");
    assertThat(storage.uploadCount()).isZero();
  }

  @Test
  void missingOriginalFilenameMarksReservedUploadFailed() {
    RecordingContentUploadStore uploadStore = new RecordingContentUploadStore();
    RecordingContentStorage storage = new RecordingContentStorage();
    ContentUploadService service =
        service(uploadStore, storage, validProbe(), EventMediaProfilePolicy.MAX_SIZE_BYTES);
    UploadContentCommand command =
        new UploadContentCommand(
            "missing-filename-1",
            "Event title",
            "Event description",
            "EDUCATION",
            new BytesUploadVideoSource(null, "video/mp4", mp4Bytes()));

    assertThatThrownBy(() -> service.upload(creatorPrincipal(), command))
        .isInstanceOf(ApiException.class)
        .satisfies(throwable -> assertApiError(throwable, ApiErrorCode.VALIDATION_ERROR));

    ContentItem failed = uploadStore.all().get(0);
    assertThat(failed.status()).isEqualTo(ContentStatus.FAILED);
    assertThat(failed.uploadFailureCode()).isEqualTo("INVALID_FILENAME");
    assertThat(storage.uploadCount()).isZero();
  }

  @Test
  void spoofedMp4ContentTypeWithBadSignatureMarksReservedUploadFailed() {
    RecordingContentUploadStore uploadStore = new RecordingContentUploadStore();
    RecordingContentStorage storage = new RecordingContentStorage();
    ContentUploadService service =
        service(uploadStore, storage, validProbe(), EventMediaProfilePolicy.MAX_SIZE_BYTES);
    UploadContentCommand command =
        new UploadContentCommand(
            "bad-signature-1",
            "Event title",
            "Event description",
            "EDUCATION",
            new BytesUploadVideoSource(
                "clip.mp4", "video/mp4", "not-an-mp4-file".getBytes(StandardCharsets.US_ASCII)));

    assertThatThrownBy(() -> service.upload(creatorPrincipal(), command))
        .isInstanceOf(ApiException.class)
        .satisfies(throwable -> assertApiError(throwable, ApiErrorCode.UPLOAD_VALIDATION_FAILED));

    ContentItem failed = uploadStore.all().get(0);
    assertThat(failed.status()).isEqualTo(ContentStatus.FAILED);
    assertThat(failed.uploadFailureCode()).isEqualTo("UNSUPPORTED_MEDIA_PROFILE");
    assertThat(storage.uploadCount()).isZero();
  }

  @Test
  void oversizeUploadUsesPayloadTooLargeAndNeverTouchesStorage() {
    RecordingContentUploadStore uploadStore = new RecordingContentUploadStore();
    RecordingContentStorage storage = new RecordingContentStorage();
    ContentUploadService service = service(uploadStore, storage, validProbe(), 12L);

    assertThatThrownBy(() -> service.upload(creatorPrincipal(), validCommand("too-large-1")))
        .isInstanceOf(ApiException.class)
        .satisfies(throwable -> assertApiError(throwable, ApiErrorCode.UPLOAD_TOO_LARGE));
    assertThat(uploadStore.all().get(0).status()).isEqualTo(ContentStatus.FAILED);
    assertThat(uploadStore.all().get(0).uploadFailureCode()).isEqualTo("UPLOAD_TOO_LARGE");
    assertThat(storage.uploadCount()).isZero();
  }

  @Test
  void storageFailureMarksUploadFailedAndAttemptsCompensation() {
    RecordingContentUploadStore uploadStore = new RecordingContentUploadStore();
    RecordingContentStorage storage = new RecordingContentStorage();
    storage.failUpload();
    ContentUploadService service =
        service(uploadStore, storage, validProbe(), EventMediaProfilePolicy.MAX_SIZE_BYTES);

    assertThatThrownBy(() -> service.upload(creatorPrincipal(), validCommand("storage-fail-1")))
        .isInstanceOf(ApiException.class)
        .satisfies(throwable -> assertApiError(throwable, ApiErrorCode.INTERNAL_ERROR));

    ContentItem failed = uploadStore.all().get(0);
    assertThat(failed.status()).isEqualTo(ContentStatus.FAILED);
    assertThat(failed.uploadFailureCode()).isEqualTo("STORAGE_UPLOAD_FAILED");
    assertThat(storage.deleteCount()).isEqualTo(1);
  }

  @Test
  void failedUploadReplayRequiresANewIdempotencyKey() {
    RecordingContentUploadStore uploadStore = new RecordingContentUploadStore();
    RecordingContentStorage storage = new RecordingContentStorage();
    ContentUploadService service = service(uploadStore, storage, validProbe(), 12L);
    UploadContentCommand command = validCommand("failed-replay-1");
    assertThatThrownBy(() -> service.upload(creatorPrincipal(), command))
        .isInstanceOf(ApiException.class);

    assertThatThrownBy(() -> service.upload(creatorPrincipal(), command))
        .isInstanceOf(ApiException.class)
        .satisfies(throwable -> assertApiError(throwable, ApiErrorCode.UPLOAD_ALREADY_FAILED));
  }

  @Test
  void concurrentDuplicateIdempotencyAttemptsCreateOneUpload() throws Exception {
    RecordingContentUploadStore uploadStore = new RecordingContentUploadStore(new CyclicBarrier(2));
    RecordingContentStorage storage = new RecordingContentStorage();
    ContentUploadService service =
        service(uploadStore, storage, validProbe(), EventMediaProfilePolicy.MAX_SIZE_BYTES);
    UploadContentCommand command = validCommand("concurrent-upload-1");
    ExecutorService executor = Executors.newFixedThreadPool(2);

    try {
      Future<ApiErrorCode> first = executor.submit(() -> uploadErrorCode(service, command));
      Future<ApiErrorCode> second = executor.submit(() -> uploadErrorCode(service, command));

      assertThat(first.get()).isIn(null, ApiErrorCode.UPLOAD_IN_PROGRESS);
      assertThat(second.get()).isIn(null, ApiErrorCode.UPLOAD_IN_PROGRESS);
    } finally {
      executor.shutdownNow();
      assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }

    assertThat(uploadStore.all()).hasSize(1);
    assertThat(storage.uploadCount()).isEqualTo(1);
    assertThat(service.upload(creatorPrincipal(), command).created()).isFalse();
  }

  private ContentUploadService service(
      RecordingContentUploadStore uploadStore,
      RecordingContentStorage storage,
      MediaProbe mediaProbe,
      long maxFileSizeBytes) {
    CreatorFollowStore creatorFollowStore = new NoopCreatorFollowStore();
    CreatorProfileStore creatorProfileStore =
        new InMemoryCreatorProfileStore(CREATOR_ID, "event-creator");
    CreatorProfileQueryService creatorProfileQueryService =
        new CreatorProfileQueryService(
            creatorProfileStore, creatorFollowStore, TRANSACTION_MANAGER);
    PublicContentQueryService publicContentQueryService =
        new PublicContentQueryService(
            new UploadBackedContentQueryStore(uploadStore),
            new EmptyContentInteractionStore(),
            storage,
            creatorProfileQueryService,
            creatorFollowStore,
            TRANSACTION_MANAGER);
    ContentUploadQueryService uploadQueryService =
        new ContentUploadQueryService(uploadStore, publicContentQueryService, TRANSACTION_MANAGER);
    return new ContentUploadService(
        uploadStore,
        creatorProfileStore,
        storage,
        mediaProbe,
        new EventMediaProfilePolicy(),
        new TestUploadSettings(maxFileSizeBytes, tempDir),
        uploadQueryService,
        TRANSACTION_MANAGER,
        CLOCK);
  }

  private static UploadContentCommand validCommand(String idempotencyKey) {
    return new UploadContentCommand(
        idempotencyKey,
        "Event title",
        "Event description",
        "EDUCATION",
        new BytesUploadVideoSource("clip.mp4", "video/mp4", mp4Bytes()));
  }

  private static MediaProbe validProbe() {
    return ignored ->
        new MediaInspection("mov,mp4,m4a,3gp,3g2,mj2", "h264", "aac", 60_000, 1080, 1920, 30d, 2);
  }

  private static byte[] mp4Bytes() {
    return "0000ftypisom0000".getBytes(StandardCharsets.US_ASCII);
  }

  private static MinisPrincipal creatorPrincipal() {
    return new MinisPrincipal(
        TestIdentities.googleIdentity("creator"), CREATOR_ID, "CREATOR", "ACTIVE");
  }

  private static MinisPrincipal viewerPrincipal() {
    return new MinisPrincipal(
        TestIdentities.googleIdentity("viewer"), VIEWER_ID, "VIEWER", "ACTIVE");
  }

  private static void assertApiError(Throwable throwable, ApiErrorCode expectedCode) {
    assertThat(((ApiException) throwable).errorCode()).isEqualTo(expectedCode);
  }

  private static ApiErrorCode uploadErrorCode(
      ContentUploadService service, UploadContentCommand command) {
    try {
      service.upload(creatorPrincipal(), command);
      return null;
    } catch (ApiException exception) {
      return exception.errorCode();
    }
  }

  private record TestUploadSettings(long maxFileSizeBytes, Path tempDirectory)
      implements ContentUploadSettings {
    @Override
    public Duration staleUploadAge() {
      return Duration.ofMinutes(30);
    }
  }

  private record BytesUploadVideoSource(String originalFilename, String contentType, byte[] bytes)
      implements UploadVideoSource {
    @Override
    public long size() {
      return bytes.length;
    }

    @Override
    public void transferTo(Path destination) throws IOException {
      Files.write(destination, bytes);
    }
  }

  private static final class RecordingContentUploadStore implements ContentUploadStore {
    private final Map<UUID, ContentItem> byId = new ConcurrentHashMap<>();
    private final Map<String, ContentItem> byCreatorAndKey = new ConcurrentHashMap<>();
    private final CyclicBarrier concurrentReservationBarrier;

    RecordingContentUploadStore() {
      this(null);
    }

    RecordingContentUploadStore(CyclicBarrier concurrentReservationBarrier) {
      this.concurrentReservationBarrier = concurrentReservationBarrier;
    }

    @Override
    public Optional<ContentItem> findById(UUID contentId) {
      return byId(contentId);
    }

    Optional<ContentItem> byId(UUID contentId) {
      return Optional.ofNullable(byId.get(contentId));
    }

    @Override
    public Optional<ContentItem> findByCreatorUserIdAndUploadIdempotencyKey(
        UUID creatorUserId, String uploadIdempotencyKey) {
      ContentItem content = byCreatorAndKey.get(uploadKey(creatorUserId, uploadIdempotencyKey));
      if (content == null && concurrentReservationBarrier != null) {
        awaitConcurrentReservationRead(concurrentReservationBarrier);
        content = byCreatorAndKey.get(uploadKey(creatorUserId, uploadIdempotencyKey));
      }
      return Optional.ofNullable(content);
    }

    @Override
    public Optional<ContentUploadView> findUploadByCreatorUserIdAndId(
        UUID creatorUserId, UUID contentId) {
      return byId(contentId)
          .filter(item -> item.creatorUserId().equals(creatorUserId))
          .filter(item -> item.uploadIdempotencyKey() != null)
          .map(RecordingContentUploadStore::toView);
    }

    @Override
    public List<ContentUploadView> findUploadsByCreatorUserId(
        UUID creatorUserId, ContentUploadCursor cursor, int limit) {
      return byId.values().stream()
          .filter(item -> item.creatorUserId().equals(creatorUserId))
          .filter(item -> item.uploadIdempotencyKey() != null)
          .filter(item -> beforeCursor(item, cursor))
          .sorted(RecordingContentUploadStore::compareNewestFirst)
          .limit(limit)
          .map(RecordingContentUploadStore::toView)
          .toList();
    }

    @Override
    public synchronized ContentItem saveAndFlush(ContentItem contentItem) {
      if (contentItem.uploadIdempotencyKey() != null) {
        ContentItem existing =
            byCreatorAndKey.get(
                uploadKey(contentItem.creatorUserId(), contentItem.uploadIdempotencyKey()));
        if (existing != null && !existing.id().equals(contentItem.id())) {
          throw new org.springframework.dao.DataIntegrityViolationException(
              "simulated concurrent upload reservation");
        }
        byCreatorAndKey.put(
            uploadKey(contentItem.creatorUserId(), contentItem.uploadIdempotencyKey()),
            contentItem);
      }
      byId.put(contentItem.id(), contentItem);
      return contentItem;
    }

    @Override
    public boolean publishedContentOwnsObject(UUID contentId, String objectKey) {
      return byId(contentId)
          .filter(item -> item.status() == ContentStatus.PUBLISHED)
          .filter(item -> item.mediaObjectKey().equals(objectKey))
          .isPresent();
    }

    List<ContentItem> all() {
      return List.copyOf(byId.values());
    }

    private static ContentUploadView toView(ContentItem item) {
      return new ContentUploadView(
          item.id(),
          item.title(),
          item.description(),
          item.category().name(),
          item.category().displayName(),
          item.status(),
          item.uploadFailureCode(),
          item.uploadFailureReason(),
          item.createdAt(),
          item.uploadStartedAt(),
          item.uploadCompletedAt(),
          item.publishedAt(),
          item.mediaContentType(),
          item.mediaSizeBytes(),
          item.durationMs());
    }

    private static boolean beforeCursor(ContentItem item, ContentUploadCursor cursor) {
      if (cursor == null) {
        return true;
      }
      return item.createdAt().isBefore(cursor.createdAt())
          || (item.createdAt().equals(cursor.createdAt())
              && item.id().compareTo(cursor.contentId()) < 0);
    }

    private static int compareNewestFirst(ContentItem left, ContentItem right) {
      int createdComparison = right.createdAt().compareTo(left.createdAt());
      if (createdComparison != 0) {
        return createdComparison;
      }
      return right.id().compareTo(left.id());
    }

    private static String uploadKey(UUID creatorUserId, String uploadIdempotencyKey) {
      return creatorUserId + ":" + uploadIdempotencyKey;
    }

    private static void awaitConcurrentReservationRead(CyclicBarrier reservationBarrier) {
      try {
        reservationBarrier.await(5, TimeUnit.SECONDS);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(exception);
      } catch (BrokenBarrierException | TimeoutException exception) {
        throw new IllegalStateException(exception);
      }
    }
  }

  private static final class UploadBackedContentQueryStore implements ContentQueryStore {
    private final RecordingContentUploadStore uploadStore;

    UploadBackedContentQueryStore(RecordingContentUploadStore uploadStore) {
      this.uploadStore = uploadStore;
    }

    @Override
    public List<PublishedContentView> findFeed(
        ContentCategory category, ContentCursor cursor, int limit) {
      return uploadStore.all().stream()
          .filter(item -> item.status() == ContentStatus.PUBLISHED)
          .filter(item -> category == null || item.category() == category)
          .map(UploadBackedContentQueryStore::toPublishedView)
          .limit(limit)
          .toList();
    }

    @Override
    public List<PublishedContentView> findCreatorContent(
        UUID creatorUserId, ContentCursor cursor, int limit) {
      return uploadStore.all().stream()
          .filter(item -> item.status() == ContentStatus.PUBLISHED)
          .filter(item -> item.creatorUserId().equals(creatorUserId))
          .map(UploadBackedContentQueryStore::toPublishedView)
          .limit(limit)
          .toList();
    }

    @Override
    public Optional<PublishedContentView> findPublishedById(UUID contentId) {
      return uploadStore
          .byId(contentId)
          .filter(item -> item.status() == ContentStatus.PUBLISHED)
          .map(UploadBackedContentQueryStore::toPublishedView);
    }

    private static PublishedContentView toPublishedView(ContentItem item) {
      return new PublishedContentView(
          item.id(),
          item.creatorUserId(),
          "event-creator",
          "Event Creator",
          null,
          true,
          item.title(),
          item.description(),
          item.category(),
          item.mediaObjectKey(),
          item.mediaContentType(),
          item.mediaSizeBytes(),
          item.durationMs(),
          item.publishedAt());
    }
  }

  private static final class EmptyContentInteractionStore implements ContentInteractionStore {
    @Override
    public int ensureLikedIfPublished(UUID userId, UUID contentId, Instant createdAt) {
      return 0;
    }

    @Override
    public boolean isLiked(UUID userId, UUID contentId) {
      return false;
    }

    @Override
    public void removeLike(UUID userId, UUID contentId) {}

    @Override
    public int ensureSavedIfPublished(UUID userId, UUID contentId, Instant createdAt) {
      return 0;
    }

    @Override
    public boolean isSaved(UUID userId, UUID contentId) {
      return false;
    }

    @Override
    public void removeSave(UUID userId, UUID contentId) {}

    @Override
    public boolean publishedContentExists(UUID contentId) {
      return false;
    }

    @Override
    public Map<UUID, Long> likeCountsByContentIds(List<UUID> contentIds) {
      return Map.of();
    }

    @Override
    public Set<UUID> likedContentIds(UUID userId, List<UUID> contentIds) {
      return Set.of();
    }

    @Override
    public Set<UUID> savedContentIds(UUID userId, List<UUID> contentIds) {
      return Set.of();
    }

    @Override
    public List<PersonalContentView> findLikedContent(
        UUID userId, ContentInteractionCursor cursor, int limit) {
      return List.of();
    }

    @Override
    public List<PersonalContentView> findSavedContent(
        UUID userId, ContentInteractionCursor cursor, int limit) {
      return List.of();
    }
  }
}
