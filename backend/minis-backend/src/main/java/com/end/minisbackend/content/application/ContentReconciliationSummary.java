package com.end.minisbackend.content.application;

public record ContentReconciliationSummary(String runId, int checkedCount, int missingCount) {
  public int exitCode() {
    return missingCount == 0 ? 0 : 1;
  }
}
