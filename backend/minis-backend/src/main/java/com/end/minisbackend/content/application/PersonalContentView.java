package com.end.minisbackend.content.application;

import java.time.Instant;

public record PersonalContentView(PublishedContentView content, Instant relationshipCreatedAt) {}
