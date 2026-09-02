package com.end.minisbackend.content.application.model.importing;

import java.util.UUID;

public record ContentImportManifestEntry(
    String assetPath,
    String importSourceKey,
    UUID creatorUserId,
    String creatorSlug,
    String title,
    String description,
    String category) {}
