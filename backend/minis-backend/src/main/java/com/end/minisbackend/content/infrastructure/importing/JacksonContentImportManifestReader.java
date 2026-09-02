package com.end.minisbackend.content.infrastructure.importing;

import com.end.minisbackend.content.application.model.importing.ContentImportException;
import com.end.minisbackend.content.application.model.importing.ContentImportManifest;
import com.end.minisbackend.content.application.port.ContentImportManifestReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class JacksonContentImportManifestReader implements ContentImportManifestReader {
  private final ObjectMapper objectMapper;

  public JacksonContentImportManifestReader(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public ContentImportManifest read(Path manifestPath) {
    if (manifestPath == null || !Files.isRegularFile(manifestPath)) {
      throw new ContentImportException("Import manifest is missing");
    }
    try {
      return objectMapper.readValue(manifestPath.toFile(), ContentImportManifest.class);
    } catch (JacksonException exception) {
      throw new ContentImportException("Import manifest could not be parsed", exception);
    }
  }

  @Override
  public Path verifiedAssetsRoot(Path assetsRoot) {
    if (assetsRoot == null) {
      throw new ContentImportException("assets root is missing");
    }
    try {
      Path realRoot = assetsRoot.toRealPath();
      if (!Files.isDirectory(realRoot)) {
        throw new ContentImportException("assets root is invalid");
      }
      return realRoot;
    } catch (IOException exception) {
      throw new ContentImportException("assets root is invalid", exception);
    }
  }

  @Override
  public Path resolveAsset(Path assetsRoot, String assetPath) {
    if (!StringUtils.hasText(assetPath)) {
      throw new ContentImportException("asset path is required");
    }
    Path relative = Path.of(assetPath.trim());
    if (relative.isAbsolute()) {
      throw new ContentImportException("asset path must be relative");
    }
    Path resolved = assetsRoot.resolve(relative).normalize();
    if (!resolved.startsWith(assetsRoot)) {
      throw new ContentImportException("asset path is invalid");
    }
    try {
      Path realAsset = resolved.toRealPath();
      if (!realAsset.startsWith(assetsRoot) || !Files.isRegularFile(realAsset)) {
        throw new ContentImportException("asset path is invalid");
      }
      return realAsset;
    } catch (IOException exception) {
      throw new ContentImportException("asset path is invalid", exception);
    }
  }
}
