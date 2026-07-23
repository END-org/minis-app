package com.end.minisbackend.identity.domain;

import java.io.Serializable;
import java.time.Instant;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/** Trusted identity data extracted from a verified Firebase Authentication ID token. */
public record VerifiedFirebaseIdentity(
    String firebaseUid,
    String email,
    boolean emailVerified,
    String displayName,
    String pictureUrl,
    String signInProvider,
    Instant issuedAt,
    Instant expiresAt)
    implements Serializable {
  private static final long serialVersionUID = 1L;
  private static final String PASSWORD_PROVIDER = "password";

  public VerifiedFirebaseIdentity {
    Assert.hasText(firebaseUid, "firebaseUid is required");
    email = normalize(email);
    displayName = normalize(displayName);
    pictureUrl = normalize(pictureUrl);
    signInProvider = normalize(signInProvider);
  }

  public boolean isEmailPasswordSignIn() {
    return PASSWORD_PROVIDER.equals(signInProvider);
  }

  private static String normalize(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }
}
