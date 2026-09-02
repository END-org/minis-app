package com.end.minisbackend.content.application.service.importing;

import com.end.minisbackend.content.application.model.ContentOperationRunId;
import com.end.minisbackend.content.application.model.importing.ContentImportEntryResult;
import com.end.minisbackend.content.application.model.importing.ContentImportException;
import com.end.minisbackend.content.application.model.importing.ContentImportManifest;
import com.end.minisbackend.content.application.model.importing.ContentImportManifestEntry;
import com.end.minisbackend.content.application.model.importing.ContentImportSummary;
import com.end.minisbackend.content.application.port.ContentCommandStore;
import com.end.minisbackend.content.application.port.ContentImportManifestReader;
import com.end.minisbackend.content.application.port.ContentStorage;
import com.end.minisbackend.content.application.port.ContentStorageException;
import com.end.minisbackend.content.application.port.MediaProbe;
import com.end.minisbackend.content.domain.enums.ContentCategory;
import com.end.minisbackend.content.domain.model.ContentItem;
import com.end.minisbackend.content.domain.model.media.MediaValidationResult;
import com.end.minisbackend.content.domain.policy.EventMediaProfilePolicy;
import com.end.minisbackend.creator.application.model.CreatorProfileView;
import com.end.minisbackend.creator.application.service.read.CreatorProfileQueryService;
import com.end.minisbackend.shared.error.ApiException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class CuratedContentImportService {
  private static final Logger LOGGER = LoggerFactory.getLogger(CuratedContentImportService.class);

  private final ContentCommandStore contentCommandStore;
  private final ContentStorage contentStorage;
  private final CreatorProfileQueryService creatorProfileQueryService;
  private final MediaProbe mediaProbe;
  private final EventMediaProfilePolicy mediaProfilePolicy;
  private final ContentImportManifestReader manifestReader;
  private final PlatformTransactionManager transactionManager;
  private final Clock clock;

  public CuratedContentImportService(
      ContentCommandStore contentCommandStore,
      ContentStorage contentStorage,
      CreatorProfileQueryService creatorProfileQueryService,
      MediaProbe mediaProbe,
      EventMediaProfilePolicy mediaProfilePolicy,
      ContentImportManifestReader manifestReader,
      PlatformTransactionManager transactionManager,
      Clock clock) {
    this.contentCommandStore = contentCommandStore;
    this.contentStorage = contentStorage;
    this.creatorProfileQueryService = creatorProfileQueryService;
    this.mediaProbe = mediaProbe;
    this.mediaProfilePolicy = mediaProfilePolicy;
    this.manifestReader = manifestReader;
    this.transactionManager = transactionManager;
    this.clock = clock;
  }

  public ContentImportSummary importManifest(Path manifestPath, Path assetsRoot, String runId) {
    String safeRunId = ContentOperationRunId.normalize(runId);
    LOGGER.info("event=curated_import_started runId={}", safeRunId);
    ContentImportManifest manifest = manifestReader.read(manifestPath);
    Path verifiedAssetsRoot = manifestReader.verifiedAssetsRoot(assetsRoot);
    List<ContentImportEntryResult> results = importEntries(manifest, verifiedAssetsRoot, safeRunId);
    ContentImportSummary summary = summarize(safeRunId, results);
    LOGGER.info(
        "event=curated_import_completed runId={} imported={} skipped={} reconciled={} failed={}",
        safeRunId,
        summary.importedCount(),
        summary.skippedCount(),
        summary.reconciledCount(),
        summary.failedCount());
    return summary;
  }

  private List<ContentImportEntryResult> importEntries(
      ContentImportManifest manifest, Path assetsRoot, String runId) {
    List<ContentImportEntryResult> results = new ArrayList<>();
    List<Path> resolvedAssets = new ArrayList<>();
    List<ContentImportManifestEntry> manifestEntries = entries(manifest);
    Set<String> sourceKeys = new HashSet<>();
    Set<String> assetPaths = new HashSet<>();
    for (ContentImportManifestEntry entry : manifestEntries) {
      try {
        validateDuplicate(
            normalizedSourceKey(entry.importSourceKey()),
            sourceKeys,
            "duplicate import source key");
        Path asset = manifestReader.resolveAsset(assetsRoot, entry.assetPath());
        validateDuplicate(asset.toString(), assetPaths, "duplicate manifest asset path");
        resolvedAssets.add(asset);
      } catch (ContentImportException | IllegalArgumentException exception) {
        results.add(failed(entry.importSourceKey(), "validation failed"));
        logAssetFailed(runId, entry.importSourceKey(), exception);
      }
    }
    if (!results.isEmpty()) {
      return results;
    }
    contentStorage.ensureBucket();
    for (int index = 0; index < manifestEntries.size(); index++) {
      ContentImportManifestEntry entry = manifestEntries.get(index);
      try {
        results.add(importEntry(entry, resolvedAssets.get(index), runId));
      } catch (ApiException
          | ContentImportException
          | ContentStorageException
          | DataAccessException
          | IllegalArgumentException exception) {
        results.add(failed(entry.importSourceKey(), "import failed"));
        logAssetFailed(runId, entry.importSourceKey(), exception);
      }
    }
    return results;
  }

  private ContentImportEntryResult importEntry(
      ContentImportManifestEntry entry, Path asset, String runId) {
    String sourceKey = normalizedSourceKey(entry.importSourceKey());
    ContentCategory category = ContentCategory.parse(entry.category());
    CreatorProfileView creator = creator(entry);
    ContentItem existing = contentCommandStore.findByImportSourceKey(sourceKey).orElse(null);
    if (existing != null) {
      return handleExisting(entry, sourceKey, creator, category, existing, runId);
    }

    long sizeBytes = fileSize(asset);
    mediaProfilePolicy.validateSize(sizeBytes);
    MediaValidationResult media = mediaProfilePolicy.validate(mediaProbe.inspect(asset), sizeBytes);
    String objectKey = ContentImportObjectKey.fromSourceKey(sourceKey);
    if (contentStorage.objectExists(objectKey)) {
      contentStorage.deleteNewlyUploadedObject(objectKey);
      LOGGER.info(
          "event=media_reconciliation_completed runId={} sourceKeyHash={}",
          runId,
          ContentImportObjectKey.fingerprint(sourceKey));
    }
    try {
      contentStorage.uploadImportedObject(objectKey, asset, media.contentType(), media.sizeBytes());
    } catch (ContentStorageException exception) {
      compensateUpload(runId, sourceKey, objectKey, exception);
      throw exception;
    }
    try {
      ContentItem imported =
          writeTransaction()
              .execute(
                  status ->
                      contentCommandStore.saveAndFlush(
                          ContentItem.publishImported(
                              creator.userId(),
                              entry.title(),
                              entry.description(),
                              category,
                              objectKey,
                              media.contentType(),
                              media.sizeBytes(),
                              media.durationMs(),
                              sourceKey,
                              Instant.now(clock))));
      LOGGER.info(
          "event=curated_asset_imported runId={} contentId={} sourceKeyHash={}",
          runId,
          imported.id(),
          ContentImportObjectKey.fingerprint(sourceKey));
      return new ContentImportEntryResult(sourceKey, "imported", "imported");
    } catch (DataAccessException | ApiException | IllegalArgumentException exception) {
      compensateUpload(runId, sourceKey, objectKey, exception);
      throw exception;
    }
  }

  private ContentImportEntryResult handleExisting(
      ContentImportManifestEntry entry,
      String sourceKey,
      CreatorProfileView creator,
      ContentCategory category,
      ContentItem existing,
      String runId) {
    if (!existing.matchesImportMetadata(
        creator.userId(), entry.title(), entry.description(), category)) {
      throw new ContentImportException("existing content metadata conflicts with manifest");
    }
    if (!contentStorage.objectExists(existing.mediaObjectKey())) {
      LOGGER.warn(
          "event=public_media_inconsistency_detected runId={} contentId={} sourceKeyHash={}",
          runId,
          existing.id(),
          ContentImportObjectKey.fingerprint(sourceKey));
      return new ContentImportEntryResult(sourceKey, "failed", "object missing");
    }
    LOGGER.info(
        "event=curated_asset_skipped runId={} contentId={} sourceKeyHash={}",
        runId,
        existing.id(),
        ContentImportObjectKey.fingerprint(sourceKey));
    return new ContentImportEntryResult(sourceKey, "skipped", "already imported");
  }

  private CreatorProfileView creator(ContentImportManifestEntry entry) {
    if (entry.creatorUserId() != null) {
      return creatorProfileQueryService.publicCreatorByUserId(entry.creatorUserId());
    }
    if (StringUtils.hasText(entry.creatorSlug())) {
      return creatorProfileQueryService.publicCreator(entry.creatorSlug());
    }
    throw new ContentImportException("creator is required");
  }

  private void compensateUpload(
      String runId, String sourceKey, String objectKey, RuntimeException importFailure) {
    LOGGER.warn(
        "event=import_compensation_attempted runId={} objectKeyHash={}",
        runId,
        ContentImportObjectKey.fingerprint(objectKey));
    if (persistedContentOwnsObject(runId, sourceKey, objectKey, importFailure)) {
      return;
    }
    try {
      contentStorage.deleteNewlyUploadedObject(objectKey);
      LOGGER.warn(
          "event=import_compensation_completed runId={} objectKeyHash={}",
          runId,
          ContentImportObjectKey.fingerprint(objectKey));
    } catch (ContentStorageException cleanupFailure) {
      importFailure.addSuppressed(cleanupFailure);
      LOGGER.error(
          "event=import_orphan_candidate_detected runId={} objectKeyHash={} errorCategory={}",
          runId,
          ContentImportObjectKey.fingerprint(objectKey),
          cleanupFailure.getClass().getSimpleName());
    }
  }

  private boolean persistedContentOwnsObject(
      String runId, String sourceKey, String objectKey, RuntimeException importFailure) {
    try {
      boolean owned =
          contentCommandStore
              .findByImportSourceKey(sourceKey)
              .map(content -> objectKey.equals(content.mediaObjectKey()))
              .orElse(false);
      if (owned) {
        LOGGER.warn(
            "event=import_compensation_skipped_existing_owner runId={} objectKeyHash={}",
            runId,
            ContentImportObjectKey.fingerprint(objectKey));
      }
      return owned;
    } catch (DataAccessException consistencyCheckFailure) {
      importFailure.addSuppressed(consistencyCheckFailure);
      LOGGER.error(
          "event=import_orphan_candidate_detected runId={} objectKeyHash={} errorCategory={}",
          runId,
          ContentImportObjectKey.fingerprint(objectKey),
          consistencyCheckFailure.getClass().getSimpleName());
      return true;
    }
  }

  private TransactionTemplate writeTransaction() {
    return new TransactionTemplate(transactionManager);
  }

  private static List<ContentImportManifestEntry> entries(ContentImportManifest manifest) {
    if (manifest == null || manifest.entries() == null) {
      return List.of();
    }
    return manifest.entries();
  }

  private static long fileSize(Path asset) {
    try {
      return Files.size(asset);
    } catch (IOException exception) {
      throw new ContentImportException("asset size could not be read", exception);
    }
  }

  private static void validateDuplicate(String value, Set<String> seen, String message) {
    if (!seen.add(value)) {
      throw new ContentImportException(message);
    }
  }

  private static String normalizedSourceKey(String sourceKey) {
    if (!StringUtils.hasText(sourceKey)) {
      throw new ContentImportException("import source key is required");
    }
    String normalized = sourceKey.trim();
    if (normalized.length() > ContentItem.IMPORT_SOURCE_KEY_MAX) {
      throw new ContentImportException("import source key exceeds maximum length");
    }
    return normalized;
  }

  private static ContentImportEntryResult failed(String sourceKey, String message) {
    return new ContentImportEntryResult(safeSourceKey(sourceKey), "failed", message);
  }

  private static String safeSourceKey(String sourceKey) {
    return StringUtils.hasText(sourceKey) ? sourceKey.trim() : "unknown";
  }

  private static ContentImportSummary summarize(
      String runId, List<ContentImportEntryResult> results) {
    return new ContentImportSummary(
        runId,
        count(results, "imported"),
        count(results, "skipped"),
        count(results, "reconciled"),
        count(results, "failed"),
        List.copyOf(results));
  }

  private static int count(List<ContentImportEntryResult> results, String status) {
    return (int) results.stream().filter(result -> status.equals(result.status())).count();
  }

  private static void logAssetFailed(String runId, String sourceKey, RuntimeException exception) {
    LOGGER.warn(
        "event=curated_asset_failed runId={} sourceKeyHash={} errorCategory={}",
        runId,
        ContentImportObjectKey.fingerprint(safeSourceKey(sourceKey)),
        exception.getClass().getSimpleName());
  }
}
