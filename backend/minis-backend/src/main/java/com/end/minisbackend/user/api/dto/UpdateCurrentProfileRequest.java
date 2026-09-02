package com.end.minisbackend.user.api.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCurrentProfileRequest(
    @Size(max = 120) String displayName,
    @Size(max = 1024)
        @Pattern(regexp = "^$|^https?://.+", message = "avatarUrl must be an absolute HTTP(S) URL")
        String avatarUrl,
    @Size(max = 500) String bio) {}
