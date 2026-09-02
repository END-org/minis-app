package com.end.minisbackend.content.api;

import io.swagger.v3.oas.annotations.media.Schema;

record UploadContentMultipartRequest(
    @Schema(implementation = UploadContentMetadataRequest.class)
        UploadContentMetadataRequest metadata,
    @Schema(type = "string", format = "binary") String video) {}
