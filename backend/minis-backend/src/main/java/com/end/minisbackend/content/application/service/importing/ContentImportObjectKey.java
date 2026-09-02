package com.end.minisbackend.content.application.service.importing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class ContentImportObjectKey {
  private ContentImportObjectKey() {}

  static String fromSourceKey(String importSourceKey) {
    return "content/imports/" + fingerprint(importSourceKey) + ".mp4";
  }

  static String fingerprint(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is required", exception);
    }
  }
}
