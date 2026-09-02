package com.end.minisbackend.content.api.dto.upload;

import com.end.minisbackend.content.domain.policy.ContentTextPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UploadContentMetadataRequest(
    @NotBlank @Size(max = ContentTextPolicy.TITLE_MAX) String title,
    @NotBlank @Size(max = ContentTextPolicy.DESCRIPTION_MAX) String description,
    @NotBlank String category) {}
