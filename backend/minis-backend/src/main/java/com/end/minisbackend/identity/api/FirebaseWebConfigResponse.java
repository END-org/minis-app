package com.end.minisbackend.identity.api;

/** Public Firebase Web SDK configuration for local and demo browser clients. */
public record FirebaseWebConfigResponse(
    String apiKey, String authDomain, String projectId, String appId, String storageBucket) {}
