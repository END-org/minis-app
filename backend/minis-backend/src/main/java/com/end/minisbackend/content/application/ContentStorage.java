package com.end.minisbackend.content.application;

import java.nio.file.Path;

public interface ContentStorage {
  void verifyAvailable();

  void ensureBucket();

  void uploadObject(String objectKey, Path sourceFile, String contentType, long sizeBytes);

  default void uploadImportedObject(
      String objectKey, Path sourceFile, String contentType, long sizeBytes) {
    uploadObject(objectKey, sourceFile, contentType, sizeBytes);
  }

  boolean objectExists(String objectKey);

  void deleteNewlyUploadedObject(String objectKey);

  SignedReadUrl signedReadUrl(String objectKey);
}
