package com.end.minisbackend.content.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.end.minisbackend.content.application.model.feed.SignedReadUrl;
import com.end.minisbackend.content.application.port.ContentStorageException;
import com.end.minisbackend.content.infrastructure.config.ContentStorageProperties;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

class MinioContentStorageIT {
  private static final DockerImageName MINIO_IMAGE =
      DockerImageName.parse("minio/minio:RELEASE.2025-09-07T16-13-09Z");
  private static final String ACCESS_KEY = "minisadmin";
  private static final String SECRET_KEY = "change-me-minio-test-secret";
  private static final String BUCKET = "minis-event-media";
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-07-17T12:00:00Z"), ZoneOffset.UTC);

  @TempDir private Path tempDir;

  @Test
  void storageCreatesBucketUploadsSignsAndSupportsRangeReads() throws Exception {
    try (GenericContainer<?> minio = minioContainer()) {
      minio.start();
      MinioContentStorage storage = storage(minio, BUCKET, true);
      Path media = tempDir.resolve("event-video.mp4");
      Files.writeString(media, "minis-media-test-data");

      storage.ensureBucket();
      storage.uploadObject(
          "content/imports/storage-test.mp4", media, "video/mp4", Files.size(media));
      SignedReadUrl signedReadUrl = storage.signedReadUrl("content/imports/storage-test.mp4");
      HttpResponse<String> response =
          HttpClient.newHttpClient()
              .send(
                  HttpRequest.newBuilder(signedReadUrl.url())
                      .header("Range", "bytes=0-4")
                      .GET()
                      .build(),
                  HttpResponse.BodyHandlers.ofString());

      assertThat(storage.objectExists("content/imports/storage-test.mp4")).isTrue();
      assertThat(signedReadUrl.expiresAt()).isEqualTo(Instant.parse("2026-07-17T12:10:00Z"));
      assertThat(signedReadUrl.url().toString()).contains("localhost");
      assertThat(signedReadUrl.url().toString()).doesNotContain("minio:9000");
      assertThat(response.statusCode()).isEqualTo(206);
      assertThat(response.headers().firstValue("Content-Range"))
          .hasValueSatisfying(value -> assertThat(value).startsWith("bytes 0-4/"));
      assertThat(response.headers().firstValue("Content-Type")).hasValue("video/mp4");

      storage.deleteNewlyUploadedObject("content/imports/storage-test.mp4");
      assertThat(storage.objectExists("content/imports/storage-test.mp4")).isFalse();
      assertThatThrownBy(() -> storage.signedReadUrl("content/imports/missing-storage-test.mp4"))
          .isInstanceOf(ContentStorageException.class)
          .hasMessage("Content object is missing");
    }
  }

  @Test
  void bucketIsNotCreatedWhenAutoCreateIsDisabled() {
    try (GenericContainer<?> minio = minioContainer()) {
      minio.start();
      MinioContentStorage storage = storage(minio, "missing-event-media", false);

      assertThatThrownBy(storage::ensureBucket).isInstanceOf(ContentStorageException.class);
    }
  }

  private static GenericContainer<?> minioContainer() {
    return new GenericContainer<>(MINIO_IMAGE)
        .withEnv("MINIO_ROOT_USER", ACCESS_KEY)
        .withEnv("MINIO_ROOT_PASSWORD", SECRET_KEY)
        .withCommand("server /data")
        .withExposedPorts(9000)
        .waitingFor(Wait.forHttp("/minio/health/ready").forPort(9000));
  }

  private static MinioContentStorage storage(
      GenericContainer<?> minio, String bucket, boolean autoCreateBucket) {
    String endpoint = "http://" + minio.getHost() + ":" + String.valueOf(minio.getMappedPort(9000));
    return new MinioContentStorage(
        new ContentStorageProperties(
            endpoint,
            endpoint,
            ACCESS_KEY,
            SECRET_KEY,
            bucket,
            Duration.ofMinutes(10),
            Duration.ofSeconds(5),
            Duration.ofSeconds(30),
            Duration.ofMinutes(2),
            autoCreateBucket),
        CLOCK);
  }
}
