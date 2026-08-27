package com.end.minisbackend.content.application;

import java.util.List;

public record ContentImportSummary(
    String runId,
    int importedCount,
    int skippedCount,
    int reconciledCount,
    int failedCount,
    List<ContentImportEntryResult> entries) {
  public ContentImportSummary {
    entries = entries == null ? List.of() : List.copyOf(entries);
  }

  public int exitCode() {
    return failedCount == 0 ? 0 : 1;
  }
}
