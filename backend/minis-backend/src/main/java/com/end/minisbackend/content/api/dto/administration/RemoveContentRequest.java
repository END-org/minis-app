package com.end.minisbackend.content.api.dto.administration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RemoveContentRequest(@NotBlank @Size(max = 300) String reason) {}
