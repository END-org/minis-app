package com.end.minisbackend.content.api;

import com.end.minisbackend.content.domain.ContentItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UploadContentMetadataRequest(
    @NotBlank @Size(max = ContentItem.TITLE_MAX) String title,
    @NotBlank @Size(max = ContentItem.DESCRIPTION_MAX) String description,
    @NotBlank String category) {}
