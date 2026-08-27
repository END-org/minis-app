package com.end.minisbackend.content.application;

import java.util.List;

public record ContentImportManifest(String version, List<ContentImportManifestEntry> entries) {
  public ContentImportManifest {
    entries = entries == null ? List.of() : List.copyOf(entries);
  }
}
