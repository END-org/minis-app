package com.end.minisbackend.content.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.end.minisbackend.TestTransactionManager;
import com.end.minisbackend.content.domain.ContentCategory;
import com.end.minisbackend.creator.application.CreatorProfileQueryService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

class CuratedContentImportServiceTest {
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-07-17T12:00:00Z"), ZoneOffset.UTC);

  @TempDir Path tempDir;

  @Test
  void validImportCreatesContentAndObjectThenReimportSkips() throws Exception {
    UUID creatorUserId = UUID.randomUUID();
    Path asset = asset("videos/approved.mp4");
    Path manifest = manifest("event-source-1", creatorUserId, "videos/approved.mp4");
    InMemoryContentCommandStore store = new InMemoryContentCommandStore();
    RecordingContentStorage storage = new RecordingContentStorage();
    CuratedContentImportService service = service(store, storage, creatorUserId);

    ContentImportSummary first = service.importManifest(manifest, tempDir, "run-1");
    ContentImportSummary second = service.importManifest(manifest, tempDir, "run-2");

    assertThat(asset).exists();
    assertThat(first.importedCount()).isEqualTo(1);
    assertThat(second.skippedCount()).isEqualTo(1);
    assertThat(store.all()).hasSize(1);
    assertThat(store.all().get(0).category()).isEqualTo(ContentCategory.EDUCATION);
    assertThat(storage.bucketEnsured()).isTrue();
    assertThat(storage.uploadCount()).isEqualTo(1);
  }

  @Test
  void duplicateSourceKeyIsRejectedBeforeStorageIo() throws Exception {
    UUID creatorUserId = UUID.randomUUID();
    asset("videos/one.mp4");
    asset("videos/two.mp4");
    Path manifest = tempDir.resolve("manifest.json");
    Files.writeString(
        manifest,
        """
        {
          "version": "test",
          "entries": [
            {
              "assetPath": "videos/one.mp4",
              "importSourceKey": "duplicate-source",
              "creatorUserId": "%s",
              "title": "One",
              "description": "Description",
              "category": "EDUCATION"
            },
            {
              "assetPath": "videos/two.mp4",
              "importSourceKey": "duplicate-source",
              "creatorUserId": "%s",
              "title": "Two",
              "description": "Description",
              "category": "EDUCATION"
            }
          ]
        }
        """
            .formatted(creatorUserId, creatorUserId));
    RecordingContentStorage storage = new RecordingContentStorage();

    ContentImportSummary summary =
        service(new InMemoryContentCommandStore(), storage, creatorUserId)
            .importManifest(manifest, tempDir, "run-duplicate");

    assertThat(summary.failedCount()).isEqualTo(1);
    assertThat(storage.bucketEnsured()).isFalse();
    assertThat(storage.uploadCount()).isZero();
  }

  @Test
  void traversalPathIsRejectedBeforeStorageIo() throws Exception {
    UUID creatorUserId = UUID.randomUUID();
    Path manifest = manifest("event-source-2", creatorUserId, "../private.mp4");
    RecordingContentStorage storage = new RecordingContentStorage();

    ContentImportSummary summary =
        service(new InMemoryContentCommandStore(), storage, creatorUserId)
            .importManifest(manifest, tempDir, "run-traversal");

    assertThat(summary.failedCount()).isEqualTo(1);
    assertThat(storage.bucketEnsured()).isFalse();
    assertThat(storage.uploadCount()).isZero();
  }

  @Test
  void databaseFailureAfterUploadDeletesOnlyNewObject() throws Exception {
    UUID creatorUserId = UUID.randomUUID();
    Path manifest = manifest("event-source-3", creatorUserId, "videos/failure.mp4");
    asset("videos/failure.mp4");
    InMemoryContentCommandStore store = new InMemoryContentCommandStore();
    store.failNextSave();
    RecordingContentStorage storage = new RecordingContentStorage();

    ContentImportSummary summary =
        service(store, storage, creatorUserId).importManifest(manifest, tempDir, "run-db-fail");

    assertThat(summary.failedCount()).isEqualTo(1);
    assertThat(store.all()).isEmpty();
    assertThat(storage.uploadCount()).isEqualTo(1);
    assertThat(storage.deleteCount()).isEqualTo(1);
  }

  @Test
  void normalizedSourceKeyMakesWhitespaceVariantIdempotent() throws Exception {
    UUID creatorUserId = UUID.randomUUID();
    Path manifest = manifest(" normalized-source ", creatorUserId, "videos/normalized.mp4");
    asset("videos/normalized.mp4");
    InMemoryContentCommandStore store = new InMemoryContentCommandStore();
    RecordingContentStorage storage = new RecordingContentStorage();
    CuratedContentImportService service = service(store, storage, creatorUserId);

    ContentImportSummary first = service.importManifest(manifest, tempDir, "run-normalized-1");
    ContentImportSummary second = service.importManifest(manifest, tempDir, "run-normalized-2");

    assertThat(first.importedCount()).isEqualTo(1);
    assertThat(second.skippedCount()).isEqualTo(1);
    assertThat(store.all()).hasSize(1);
    assertThat(store.all().get(0).importSourceKey()).isEqualTo("normalized-source");
    assertThat(storage.uploadCount()).isEqualTo(1);
    assertThat(storage.deleteCount()).isZero();
  }

  @Test
  void emptyAssetIsRejectedBeforeMediaProbeOrStorageUpload() throws Exception {
    UUID creatorUserId = UUID.randomUUID();
    Path manifest = manifest("empty-source", creatorUserId, "videos/empty.mp4");
    Path emptyAsset = tempDir.resolve("videos/empty.mp4");
    Files.createDirectories(emptyAsset.getParent());
    Files.createFile(emptyAsset);
    AtomicInteger probeCount = new AtomicInteger();
    RecordingContentStorage storage = new RecordingContentStorage();

    ContentImportSummary summary =
        service(
                new InMemoryContentCommandStore(),
                storage,
                creatorUserId,
                ignored -> {
                  probeCount.incrementAndGet();
                  return validInspection();
                })
            .importManifest(manifest, tempDir, "run-empty");

    assertThat(summary.failedCount()).isEqualTo(1);
    assertThat(probeCount).hasValue(0);
    assertThat(storage.uploadCount()).isZero();
  }

  @Test
  void concurrentDatabaseWinnerPreventsCompensationFromDeletingOwnedObject() throws Exception {
    UUID creatorUserId = UUID.randomUUID();
    Path manifest = manifest("concurrent-source", creatorUserId, "videos/concurrent.mp4");
    asset("videos/concurrent.mp4");
    InMemoryContentCommandStore store = new InMemoryContentCommandStore();
    store.failNextSaveAfterConcurrentCommit();
    RecordingContentStorage storage = new RecordingContentStorage();

    ContentImportSummary summary =
        service(store, storage, creatorUserId).importManifest(manifest, tempDir, "run-concurrent");

    assertThat(summary.failedCount()).isEqualTo(1);
    assertThat(store.all()).hasSize(1);
    assertThat(storage.deleteCount()).isZero();
    assertThat(storage.hasObject(store.all().get(0).mediaObjectKey())).isTrue();
  }

  @Test
  void failedCompensationLeavesFailedResultAndDetectableOrphanCandidate() throws Exception {
    UUID creatorUserId = UUID.randomUUID();
    Path manifest = manifest("cleanup-source", creatorUserId, "videos/cleanup.mp4");
    asset("videos/cleanup.mp4");
    InMemoryContentCommandStore store = new InMemoryContentCommandStore();
    store.failNextSave();
    RecordingContentStorage storage = new RecordingContentStorage();
    storage.failDelete();

    ContentImportSummary summary =
        service(store, storage, creatorUserId).importManifest(manifest, tempDir, "run-cleanup");

    assertThat(summary.failedCount()).isEqualTo(1);
    assertThat(store.all()).isEmpty();
    assertThat(storage.deleteCount()).isEqualTo(1);
    assertThat(storage.objectCount()).isEqualTo(1);
  }

  @Test
  void uncertainUploadFailureNeverCreatesMetadataOrReportsSuccess() throws Exception {
    UUID creatorUserId = UUID.randomUUID();
    Path manifest = manifest("upload-failure", creatorUserId, "videos/upload-failure.mp4");
    asset("videos/upload-failure.mp4");
    InMemoryContentCommandStore store = new InMemoryContentCommandStore();
    RecordingContentStorage storage = new RecordingContentStorage();
    storage.failUpload();

    ContentImportSummary summary =
        service(store, storage, creatorUserId).importManifest(manifest, tempDir, "run-upload-fail");

    assertThat(summary.failedCount()).isEqualTo(1);
    assertThat(summary.importedCount()).isZero();
    assertThat(store.all()).isEmpty();
    assertThat(storage.deleteCount()).isEqualTo(1);
  }

  private CuratedContentImportService service(
      InMemoryContentCommandStore store, RecordingContentStorage storage, UUID creatorUserId) {
    return service(store, storage, creatorUserId, ignored -> validInspection());
  }

  private CuratedContentImportService service(
      InMemoryContentCommandStore store,
      RecordingContentStorage storage,
      UUID creatorUserId,
      MediaProbe mediaProbe) {
    CreatorProfileQueryService creatorService =
        new CreatorProfileQueryService(
            new InMemoryCreatorProfileStore(creatorUserId, "event-creator"),
            new NoopCreatorFollowStore(),
            new TestTransactionManager());
    return new CuratedContentImportService(
        store,
        storage,
        creatorService,
        mediaProbe,
        new EventMediaProfilePolicy(),
        new ObjectMapper(),
        new TestTransactionManager(),
        CLOCK);
  }

  private static MediaInspection validInspection() {
    return new MediaInspection(
        "mov,mp4,m4a,3gp,3g2,mj2", "h264", "aac", 60_000, 1080, 1920, 30d, 2);
  }

  private Path manifest(String sourceKey, UUID creatorUserId, String assetPath) throws Exception {
    Path manifest = tempDir.resolve(sourceKey + ".json");
    Files.writeString(
        manifest,
        """
        {
          "version": "test",
          "entries": [
            {
              "assetPath": "%s",
              "importSourceKey": "%s",
              "creatorUserId": "%s",
              "title": "Event title",
              "description": "Event description",
              "category": "EDUCATION"
            }
          ]
        }
        """
            .formatted(assetPath, sourceKey, creatorUserId));
    return manifest;
  }

  private Path asset(String relativePath) throws Exception {
    Path asset = tempDir.resolve(relativePath);
    Files.createDirectories(asset.getParent());
    Files.writeString(asset, "not real video because media probe is isolated");
    return asset;
  }
}
