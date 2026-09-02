package com.end.minisbackend;

import com.end.minisbackend.identity.domain.model.VerifiedFirebaseIdentity;
import java.time.Instant;

public final class TestIdentities {
  private TestIdentities() {}

  public static VerifiedFirebaseIdentity emailPasswordIdentity(
      String firebaseUid, boolean verified) {
    return new VerifiedFirebaseIdentity(
        firebaseUid,
        firebaseUid + "@example.com",
        verified,
        "Firebase " + firebaseUid,
        "https://example.com/" + firebaseUid + ".png",
        "password",
        Instant.parse("2026-07-17T12:00:00Z"),
        Instant.parse("2026-07-17T13:00:00Z"));
  }

  public static VerifiedFirebaseIdentity googleIdentity(String firebaseUid) {
    return new VerifiedFirebaseIdentity(
        firebaseUid,
        firebaseUid + "@example.com",
        true,
        "Google " + firebaseUid,
        "https://example.com/" + firebaseUid + ".png",
        "google.com",
        Instant.parse("2026-07-17T12:00:00Z"),
        Instant.parse("2026-07-17T13:00:00Z"));
  }
}
