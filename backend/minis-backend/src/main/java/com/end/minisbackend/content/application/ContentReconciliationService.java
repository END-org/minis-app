package com.end.minisbackend.content.application;

import com.end.minisbackend.content.domain.ContentItem;
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
  private static final Logger LOGGER = LoggerFactory.getLogger(ContentReconciliationService.class);

  private final ContentCommandStore contentCommandStore;
  private final ContentStorage contentStorage;
  private final PlatformTransactionManager transactionManager;

  public ContentReconciliationService(
      ContentCommandStore contentCommandStore,
      ContentStorage contentStorage,
      PlatformTransactionManager transactionManager) {
    this.contentCommandStore = contentCommandStore;
    this.contentStorage = contentStorage;
    this.transactionManager = transactionManager;
  }

  public ContentReconciliationSummary reconcile(String runId) {
    String safeRunId = ContentOperationRunId.normalize(runId);
    UUID afterContentId = null;
    int checked = 0;
    int missing = 0;

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
        "event=media_reconciliation_completed runId={} checked={} missing={}",
        safeRunId,
        checked,
        missing);
    return new ContentReconciliationSummary(safeRunId, checked, missing);
  }

  private TransactionTemplate readOnlyTransaction() {
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);
    return transaction;
  }
}
