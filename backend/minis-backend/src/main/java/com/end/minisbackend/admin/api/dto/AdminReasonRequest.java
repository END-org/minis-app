package com.end.minisbackend.admin.api.dto;

import jakarta.validation.constraints.Size;

public record AdminReasonRequest(@Size(max = 300) String reason) {}
