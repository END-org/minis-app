package com.end.minisbackend.content.application;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

final class RecordingContentStorage implements ContentStorage {
  private final Set<String> objects = new HashSet<>();
  private int uploadCount;
  private int deleteCount;
  private boolean bucketEnsured;

  @Override
  public void ensureBucket() {
    bucketEnsured = true;
  }

  @Override
  public void uploadImportedObject(
      String objectKey, Path sourceFile, String contentType, long sizeBytes) {
    uploadCount++;
    objects.add(objectKey);
  }

  @Override
  public boolean objectExists(String objectKey) {
    return objects.contains(objectKey);
  }

  @Override
  public void deleteNewlyUploadedObject(String objectKey) {
    deleteCount++;
    objects.remove(objectKey);
  }

  @Override
  public SignedReadUrl signedReadUrl(String objectKey) {
    return new SignedReadUrl(
        java.net.URI.create("http://localhost:9000/minis-event-media/" + objectKey),
        Instant.parse("2026-07-17T12:10:00Z"));
  }

  int uploadCount() {
    return uploadCount;
  }

  int deleteCount() {
    return deleteCount;
  }

  boolean bucketEnsured() {
    return bucketEnsured;
  }

  void putObject(String objectKey) {
    objects.add(objectKey);
  }
}
