package com.end.minisbackend.identity.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Firebase Admin runtime configuration loaded from environment-backed Spring properties. */
@ConfigurationProperties(prefix = "minis.firebase")
public record FirebaseAdminProperties(String projectId) {}
