package com.end.minisbackend.content.application.port;

import com.end.minisbackend.content.application.model.importing.ContentImportManifest;
import java.nio.file.Path;

/** Reads and validates the privately supplied curated-content manifest and its asset paths. */
public interface ContentImportManifestReader {
  ContentImportManifest read(Path manifestPath);

  Path verifiedAssetsRoot(Path assetsRoot);

  Path resolveAsset(Path assetsRoot, String assetPath);
}
