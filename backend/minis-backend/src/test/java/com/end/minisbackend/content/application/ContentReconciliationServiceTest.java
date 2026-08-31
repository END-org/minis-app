package com.end.minisbackend.content.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.end.minisbackend.TestTransactionManager;
import com.end.minisbackend.content.domain.ContentCategory;
import com.end.minisbackend.content.domain.ContentItem;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContentReconciliationServiceTest {
  @Test
  void reconcileReportsPublishedObjectsThatAreMissingFromStorage() {
    InMemoryContentCommandStore store = new InMemoryContentCommandStore();
    RecordingContentStorage storage = new RecordingContentStorage();
    ContentItem existing = contentItem("content/imports/existing.mp4");
    ContentItem missing = contentItem("content/imports/missing.mp4");
    store.saveAndFlush(existing);
    store.saveAndFlush(missing);
    storage.putObject(existing.mediaObjectKey());

    ContentReconciliationSummary summary = service(store, storage).reconcile(" event-readiness ");

    assertThat(summary.runId()).isEqualTo("event-readiness");
    assertThat(summary.checkedCount()).isEqualTo(2);
    assertThat(summary.missingCount()).isEqualTo(1);
    assertThat(summary.exitCode()).isEqualTo(1);
  }

  @Test
  void reconcileSucceedsWhenAllPublishedObjectsExist() {
    InMemoryContentCommandStore store = new InMemoryContentCommandStore();
    RecordingContentStorage storage = new RecordingContentStorage();
    ContentItem first = contentItem("content/imports/first.mp4");
    ContentItem second = contentItem("content/imports/second.mp4");
    store.saveAndFlush(first);
    store.saveAndFlush(second);
    storage.putObject(first.mediaObjectKey());
    storage.putObject(second.mediaObjectKey());

    ContentReconciliationSummary summary = service(store, storage).reconcile(null);

    assertThat(summary.runId()).isNotBlank();
    assertThat(summary.checkedCount()).isEqualTo(2);
    assertThat(summary.missingCount()).isZero();
    assertThat(summary.exitCode()).isZero();
  }

  @Test
  void reconcileReadsPublishedContentInBoundedBatches() {
    InMemoryContentCommandStore store = new InMemoryContentCommandStore();
    RecordingContentStorage storage = new RecordingContentStorage();
    for (int index = 0; index < 205; index++) {
      ContentItem item = contentItem("content/imports/batched-" + index + ".mp4");
      store.saveAndFlush(item);
      storage.putObject(item.mediaObjectKey());
    }

    ContentReconciliationSummary summary = service(store, storage).reconcile("bounded-run");

    assertThat(summary.checkedCount()).isEqualTo(205);
    assertThat(summary.missingCount()).isZero();
    assertThat(store.largestRequestedBatch()).isEqualTo(100);
    assertThat(store.publishedBatchQueryCount()).isEqualTo(4);
  }

  @Test
  void reconciliationRejectsUnsafeRunIdBeforeLoggingOrStorageAccess() {
    InMemoryContentCommandStore store = new InMemoryContentCommandStore();
    RecordingContentStorage storage = new RecordingContentStorage();

    assertThatThrownBy(() -> service(store, storage).reconcile("unsafe\nlog-entry"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("content operation run ID is invalid");
    assertThat(store.publishedBatchQueryCount()).isZero();
  }

  private static ContentReconciliationService service(
      InMemoryContentCommandStore store, RecordingContentStorage storage) {
    return new ContentReconciliationService(store, storage, new TestTransactionManager());
  }

  private static ContentItem contentItem(String objectKey) {
    return ContentItem.publishImported(
        UUID.randomUUID(),
        "Event title",
        "Event description",
        ContentCategory.EDUCATION,
        objectKey,
        "video/mp4",
        1024L,
        1000L,
        objectKey,
        Instant.parse("2026-07-17T12:00:00Z"));
  }
}
