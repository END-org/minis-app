package com.end.minisbackend.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GrantCreatorRoleRequest(
    @NotBlank @Size(max = 120) String slug,
    @NotBlank @Size(max = 120) String displayName,
    @Size(max = 500) String bio,
    @Size(max = 1024)
        @Pattern(regexp = "^$|^https?://.+", message = "avatarUrl must be an absolute HTTP(S) URL")
        String avatarUrl,
    @Size(max = 300) String reason) {}
