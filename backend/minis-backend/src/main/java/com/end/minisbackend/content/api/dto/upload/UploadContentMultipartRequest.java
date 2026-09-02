package com.end.minisbackend.content.api.dto.upload;

import io.swagger.v3.oas.annotations.media.Schema;

public record UploadContentMultipartRequest(
    @Schema(implementation = UploadContentMetadataRequest.class)
        UploadContentMetadataRequest metadata,
    @Schema(type = "string", format = "binary") String video) {}
