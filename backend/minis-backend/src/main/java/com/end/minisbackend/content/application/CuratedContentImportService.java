package com.end.minisbackend.content.application;

import com.end.minisbackend.content.domain.ContentCategory;
import com.end.minisbackend.content.domain.ContentItem;
import com.end.minisbackend.creator.application.CreatorProfileQueryService;
import com.end.minisbackend.creator.application.CreatorProfileView;
import com.end.minisbackend.shared.error.ApiException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class CuratedContentImportService {
  private static final Logger LOGGER = LoggerFactory.getLogger(CuratedContentImportService.class);

  private final ContentCommandStore contentCommandStore;
  private final ContentStorage contentStorage;
  private final CreatorProfileQueryService creatorProfileQueryService;
  private final MediaProbe mediaProbe;
  private final EventMediaProfilePolicy mediaProfilePolicy;
  private final ObjectMapper objectMapper;
  private final PlatformTransactionManager transactionManager;
  private final Clock clock;

  public CuratedContentImportService(
      ContentCommandStore contentCommandStore,
      ContentStorage contentStorage,
      CreatorProfileQueryService creatorProfileQueryService,
      MediaProbe mediaProbe,
      EventMediaProfilePolicy mediaProfilePolicy,
      ObjectMapper objectMapper,
      PlatformTransactionManager transactionManager,
      Clock clock) {
    this.contentCommandStore = contentCommandStore;
    this.contentStorage = contentStorage;
    this.creatorProfileQueryService = creatorProfileQueryService;
    this.mediaProbe = mediaProbe;
    this.mediaProfilePolicy = mediaProfilePolicy;
    this.objectMapper = objectMapper;
    this.transactionManager = transactionManager;
    this.clock = clock;
  }

  public ContentImportSummary importManifest(Path manifestPath, Path assetsRoot, String runId) {
    String safeRunId = StringUtils.hasText(runId) ? runId.trim() : UUID.randomUUID().toString();
    LOGGER.info("event=curated_import_started runId={}", safeRunId);
    ContentImportManifest manifest = readManifest(manifestPath);
    Path normalizedAssetsRoot = assetsRoot.toAbsolutePath().normalize();
    List<ContentImportEntryResult> results =
        importEntries(manifest, normalizedAssetsRoot, safeRunId);
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
        validateDuplicate(entry.importSourceKey(), sourceKeys, "duplicate import source key");
        Path asset = resolveAsset(assetsRoot, entry.assetPath());
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
          | DataIntegrityViolationException
          | IllegalArgumentException exception) {
        results.add(failed(entry.importSourceKey(), "import failed"));
        logAssetFailed(runId, entry.importSourceKey(), exception);
      }
    }
    return results;
  }

  private ContentImportEntryResult importEntry(
      ContentImportManifestEntry entry, Path asset, String runId) {
    ContentCategory category = ContentCategory.parse(entry.category());
    CreatorProfileView creator = creator(entry);
    ContentItem existing =
        contentCommandStore.findByImportSourceKey(entry.importSourceKey()).orElse(null);
    if (existing != null) {
      return handleExisting(entry, creator, category, existing, runId);
    }

    long sizeBytes = fileSize(asset);
    MediaValidationResult media = mediaProfilePolicy.validate(mediaProbe.inspect(asset), sizeBytes);
    String objectKey = objectKey(entry.importSourceKey());
    if (contentStorage.objectExists(objectKey)) {
      contentStorage.deleteNewlyUploadedObject(objectKey);
      LOGGER.info(
          "event=media_reconciliation_completed runId={} sourceKeyHash={}",
          runId,
          sourceHash(entry.importSourceKey()));
    }
    contentStorage.uploadImportedObject(objectKey, asset, media.contentType(), media.sizeBytes());
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
                              entry.importSourceKey(),
                              Instant.now(clock))));
      LOGGER.info(
          "event=curated_asset_imported runId={} contentId={} sourceKeyHash={}",
          runId,
          imported.id(),
          sourceHash(entry.importSourceKey()));
      return new ContentImportEntryResult(entry.importSourceKey(), "imported", "imported");
    } catch (DataIntegrityViolationException | ApiException | IllegalArgumentException exception) {
      compensateUpload(runId, objectKey);
      throw exception;
    }
  }

  private ContentImportEntryResult handleExisting(
      ContentImportManifestEntry entry,
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
          sourceHash(entry.importSourceKey()));
      return new ContentImportEntryResult(entry.importSourceKey(), "failed", "object missing");
    }
    LOGGER.info(
        "event=curated_asset_skipped runId={} contentId={} sourceKeyHash={}",
        runId,
        existing.id(),
        sourceHash(entry.importSourceKey()));
    return new ContentImportEntryResult(entry.importSourceKey(), "skipped", "already imported");
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

  private void compensateUpload(String runId, String objectKey) {
    LOGGER.warn(
        "event=import_compensation_attempted runId={} objectKeyHash={}",
        runId,
        sourceHash(objectKey));
    contentStorage.deleteNewlyUploadedObject(objectKey);
    LOGGER.warn(
        "event=import_compensation_completed runId={} objectKeyHash={}",
        runId,
        sourceHash(objectKey));
  }

  private TransactionTemplate writeTransaction() {
    return new TransactionTemplate(transactionManager);
  }

  private ContentImportManifest readManifest(Path manifestPath) {
    if (manifestPath == null || !Files.isRegularFile(manifestPath)) {
      throw new ContentImportException("Import manifest is missing");
    }
    try {
      return objectMapper.readValue(manifestPath.toFile(), ContentImportManifest.class);
    } catch (JacksonException exception) {
      throw new ContentImportException("Import manifest could not be parsed", exception);
    }
  }

  private static List<ContentImportManifestEntry> entries(ContentImportManifest manifest) {
    if (manifest == null || manifest.entries() == null) {
      return List.of();
    }
    return manifest.entries();
  }

  private static Path resolveAsset(Path assetsRoot, String assetPath) {
    if (!StringUtils.hasText(assetPath)) {
      throw new ContentImportException("asset path is required");
    }
    Path relative = Path.of(assetPath.trim());
    if (relative.isAbsolute()) {
      throw new ContentImportException("asset path must be relative");
    }
    Path resolved = assetsRoot.resolve(relative).normalize();
    if (!resolved.startsWith(assetsRoot) || !Files.isRegularFile(resolved)) {
      throw new ContentImportException("asset path is invalid");
    }
    return resolved;
  }

  private static long fileSize(Path asset) {
    try {
      return Files.size(asset);
    } catch (IOException exception) {
      throw new ContentImportException("asset size could not be read", exception);
    }
  }

  private static void validateDuplicate(String value, Set<String> seen, String message) {
    if (!StringUtils.hasText(value) || !seen.add(value.trim())) {
      throw new ContentImportException(message);
    }
  }

  private static String objectKey(String importSourceKey) {
    return "content/imports/" + sourceHash(importSourceKey) + ".mp4";
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
        sourceHash(safeSourceKey(sourceKey)),
        exception.getClass().getSimpleName());
  }

  private static String sourceHash(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of()
          .formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is required", exception);
    }
  }
}
