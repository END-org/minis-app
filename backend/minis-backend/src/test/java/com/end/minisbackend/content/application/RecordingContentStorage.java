package com.end.minisbackend.content.application;

import com.end.minisbackend.content.application.model.feed.SignedReadUrl;
import com.end.minisbackend.content.application.port.ContentStorage;
import com.end.minisbackend.content.application.port.ContentStorageException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

final class RecordingContentStorage implements ContentStorage {
  private final Set<String> objects = new HashSet<>();
  private int uploadCount;
  private int deleteCount;
  private boolean bucketEnsured;
  private boolean failUpload;
  private boolean failDelete;

  @Override
  public void verifyAvailable() {}

  @Override
  public void ensureBucket() {
    bucketEnsured = true;
  }

  @Override
  public void uploadObject(String objectKey, Path sourceFile, String contentType, long sizeBytes) {
    uploadCount++;
    if (failUpload) {
      throw new ContentStorageException("simulated upload failure");
    }
    objects.add(objectKey);
  }

  @Override
  public boolean objectExists(String objectKey) {
    return objects.contains(objectKey);
  }

  @Override
  public void deleteNewlyUploadedObject(String objectKey) {
    deleteCount++;
    if (failDelete) {
      throw new ContentStorageException("simulated cleanup failure");
    }
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

  boolean hasObject(String objectKey) {
    return objects.contains(objectKey);
  }

  int objectCount() {
    return objects.size();
  }

  void failUpload() {
    failUpload = true;
  }

  void failDelete() {
    failDelete = true;
  }
}
