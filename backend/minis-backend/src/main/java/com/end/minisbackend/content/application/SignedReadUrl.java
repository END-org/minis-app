package com.end.minisbackend.content.application;

import java.net.URI;
import java.time.Instant;

public record SignedReadUrl(URI url, Instant expiresAt) {}
