package com.end.minisbackend.content.application.model.feed;

import java.time.Instant;

public record PersonalContentView(PublishedContentView content, Instant relationshipCreatedAt) {}
