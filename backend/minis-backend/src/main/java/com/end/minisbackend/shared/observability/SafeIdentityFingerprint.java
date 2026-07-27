package com.end.minisbackend.shared.observability;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.util.StringUtils;

/** Produces stable non-reversible identifiers for sensitive external identity values. */
public final class SafeIdentityFingerprint {
  private static final int FINGERPRINT_HEX_LENGTH = 16;

  private SafeIdentityFingerprint() {}

  public static String firebaseUid(String firebaseUid) {
    if (!StringUtils.hasText(firebaseUid)) {
      return "unknown";
    }
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256")
              .digest(firebaseUid.trim().getBytes(StandardCharsets.UTF_8));
      return "sha256:" + HexFormat.of().formatHex(digest).substring(0, FINGERPRINT_HEX_LENGTH);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(
          "SHA-256 is required for safe identity fingerprints", exception);
    }
  }
}
