package com.end.minisbackend.content.application;

import com.end.minisbackend.content.domain.ContentItem;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class ContentReconciliationService {
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
    String safeRunId = StringUtils.hasText(runId) ? runId.trim() : UUID.randomUUID().toString();
    List<ContentItem> published =
        readOnlyTransaction().execute(status -> contentCommandStore.findPublishedItems());
    int missing = 0;
    for (ContentItem item : published) {
      if (!contentStorage.objectExists(item.mediaObjectKey())) {
        missing++;
        LOGGER.warn(
            "event=public_media_inconsistency_detected runId={} contentId={}",
            safeRunId,
            item.id());
      }
    }
    LOGGER.info(
        "event=media_reconciliation_completed runId={} checked={} missing={}",
        safeRunId,
        published.size(),
        missing);
    return new ContentReconciliationSummary(safeRunId, published.size(), missing);
  }

  private TransactionTemplate readOnlyTransaction() {
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);
    return transaction;
  }
}
