package com.end.minisbackend.content.application.service.reconciliation;

import com.end.minisbackend.content.application.model.ContentOperationRunId;
import com.end.minisbackend.content.application.model.reconciliation.ContentReconciliationSummary;
import com.end.minisbackend.content.application.port.ContentCommandStore;
import com.end.minisbackend.content.application.port.ContentStorage;
import com.end.minisbackend.content.domain.enums.ContentUploadFailureCode;
import com.end.minisbackend.content.domain.model.ContentItem;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ContentReconciliationService {
  private static final int BATCH_SIZE = 100;
  private static final Duration DEFAULT_STALE_UPLOAD_AGE = Duration.ofMinutes(30);
  private static final Logger LOGGER = LoggerFactory.getLogger(ContentReconciliationService.class);

  private final ContentCommandStore contentCommandStore;
  private final ContentStorage contentStorage;
  private final PlatformTransactionManager transactionManager;
  private final Clock clock;

  public ContentReconciliationService(
      ContentCommandStore contentCommandStore,
      ContentStorage contentStorage,
      PlatformTransactionManager transactionManager,
      Clock clock) {
    this.contentCommandStore = contentCommandStore;
    this.contentStorage = contentStorage;
    this.transactionManager = transactionManager;
    this.clock = clock;
  }

  public ContentReconciliationSummary reconcile(String runId) {
    return reconcile(runId, DEFAULT_STALE_UPLOAD_AGE);
  }

  public ContentReconciliationSummary reconcile(String runId, Duration staleUploadAge) {
    String safeRunId = ContentOperationRunId.normalize(runId);
    UUID afterContentId = null;
    int checked = 0;
    int missing = 0;
    int staleUploads = failStaleUploads(safeRunId, staleUploadAge);

    while (true) {
      UUID batchCursor = afterContentId;
      List<ContentItem> published =
          readOnlyTransaction()
              .execute(
                  status -> contentCommandStore.findPublishedItemsAfter(batchCursor, BATCH_SIZE));
      if (published.isEmpty()) {
        break;
      }
      for (ContentItem item : published) {
        checked++;
        if (!contentStorage.objectExists(item.mediaObjectKey())) {
          missing++;
          LOGGER.warn(
              "event=public_media_inconsistency_detected runId={} contentId={}",
              safeRunId,
              item.id());
        }
      }
      afterContentId = published.get(published.size() - 1).id();
    }
    LOGGER.info(
        "event=media_reconciliation_completed runId={} checked={} missing={} staleUploads={}",
        safeRunId,
        checked,
        missing,
        staleUploads);
    return new ContentReconciliationSummary(safeRunId, checked, missing, staleUploads);
  }

  private int failStaleUploads(String runId, Duration staleUploadAge) {
    Duration boundedStaleAge =
        staleUploadAge == null || staleUploadAge.isZero() || staleUploadAge.isNegative()
            ? DEFAULT_STALE_UPLOAD_AGE
            : staleUploadAge;
    Instant cutoff = Instant.now(clock).minus(boundedStaleAge);
    int failed = 0;
    while (true) {
      List<ContentItem> staleUploads =
          readOnlyTransaction()
              .execute(
                  status -> contentCommandStore.findStaleUploadingItemsBefore(cutoff, BATCH_SIZE));
      if (staleUploads.isEmpty()) {
        return failed;
      }
      for (ContentItem staleUpload : staleUploads) {
        UUID contentId = staleUpload.id();
        Boolean changed =
            writeTransaction()
                .execute(
                    status ->
                        contentCommandStore
                            .findById(contentId)
                            .map(
                                content -> {
                                  boolean marked =
                                      content.failUpload(
                                          ContentUploadFailureCode.STALE_UPLOAD,
                                          null,
                                          Instant.now(clock));
                                  if (marked) {
                                    contentCommandStore.saveAndFlush(content);
                                  }
                                  return marked;
                                })
                            .orElse(false));
        if (Boolean.TRUE.equals(changed)) {
          failed++;
          LOGGER.warn("event=stale_upload_failed runId={} contentId={}", runId, contentId);
        }
      }
    }
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
