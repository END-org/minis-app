package com.end.minisbackend.identity.application;

import java.util.UUID;

/** Minimal local account state needed to build the authenticated principal. */
public record LocalUserIdentity(UUID id, String role, String status) {}
