package com.end.minisbackend.identity.infrastructure.config;

import com.end.minisbackend.identity.application.port.FirebaseWebConfigProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Public Firebase Web SDK values exposed only in the local profile. */
@ConfigurationProperties(prefix = "minis.firebase.web")
public record FirebaseWebConfigProperties(
    String apiKey, String authDomain, String projectId, String appId, String storageBucket)
    implements FirebaseWebConfigProvider {}
