package com.end.minisbackend.content.application.model.feed;

import java.net.URI;
import java.time.Instant;

public record SignedReadUrl(URI url, Instant expiresAt) {}
