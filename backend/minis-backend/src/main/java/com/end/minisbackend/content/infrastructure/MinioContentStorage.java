package com.end.minisbackend.content.infrastructure;

import com.end.minisbackend.content.application.ContentStorage;
import com.end.minisbackend.content.application.ContentStorageException;
import com.end.minisbackend.content.application.SignedReadUrl;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import io.minio.http.Method;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;

@Component
class MinioContentStorage implements ContentStorage {
  private final ContentStorageProperties properties;
  private final Clock clock;
  private final MinioClient objectClient;
  private final MinioClient signingClient;

  MinioContentStorage(ContentStorageProperties properties, Clock clock) {
    this.properties = properties;
    this.clock = clock;
    properties.requireConfigured();
    OkHttpClient httpClient = httpClient(properties);
    this.objectClient = client(properties.internalEndpoint(), properties, httpClient);
    this.signingClient = client(properties.publicEndpoint(), properties, httpClient);
  }

  @Override
  public void verifyAvailable() {
    if (!bucketExists()) {
      throw new ContentStorageException("Content storage bucket does not exist");
    }
  }

  @Override
  public void ensureBucket() {
    try {
      boolean exists = bucketExists();
      if (!exists && properties.autoCreateBucket()) {
        objectClient.makeBucket(MakeBucketArgs.builder().bucket(properties.bucket()).build());
        return;
      }
      if (!exists) {
        throw new ContentStorageException("Content storage bucket does not exist");
      }
    } catch (ContentStorageException exception) {
      throw exception;
    } catch (ErrorResponseException
        | InsufficientDataException
        | InternalException
        | InvalidKeyException
        | InvalidResponseException
        | IOException
        | NoSuchAlgorithmException
        | ServerException
        | XmlParserException exception) {
      throw new ContentStorageException("Content storage bucket check failed", exception);
    }
  }

  @Override
  public void uploadImportedObject(
      String objectKey, Path sourceFile, String contentType, long sizeBytes) {
    try (InputStream inputStream = Files.newInputStream(sourceFile)) {
      objectClient.putObject(
          PutObjectArgs.builder().bucket(properties.bucket()).object(objectKey).stream(
                  inputStream, sizeBytes, -1)
              .contentType(contentType)
              .build());
    } catch (ErrorResponseException
        | InsufficientDataException
        | InternalException
        | InvalidKeyException
        | InvalidResponseException
        | IOException
        | NoSuchAlgorithmException
        | ServerException
        | XmlParserException exception) {
      throw new ContentStorageException("Content object upload failed", exception);
    }
  }

  @Override
  public boolean objectExists(String objectKey) {
    try {
      objectClient.statObject(
          StatObjectArgs.builder().bucket(properties.bucket()).object(objectKey).build());
      return true;
    } catch (ErrorResponseException exception) {
      if ("NoSuchKey".equals(exception.errorResponse().code())
          || "NoSuchBucket".equals(exception.errorResponse().code())) {
        return false;
      }
      throw new ContentStorageException("Content object metadata read failed", exception);
    } catch (InsufficientDataException
        | InternalException
        | InvalidKeyException
        | InvalidResponseException
        | IOException
        | NoSuchAlgorithmException
        | ServerException
        | XmlParserException exception) {
      throw new ContentStorageException("Content object metadata read failed", exception);
    }
  }

  @Override
  public void deleteNewlyUploadedObject(String objectKey) {
    try {
      objectClient.removeObject(
          RemoveObjectArgs.builder().bucket(properties.bucket()).object(objectKey).build());
    } catch (ErrorResponseException
        | InsufficientDataException
        | InternalException
        | InvalidKeyException
        | InvalidResponseException
        | IOException
        | NoSuchAlgorithmException
        | ServerException
        | XmlParserException exception) {
      throw new ContentStorageException("Content object compensation deletion failed", exception);
    }
  }

  @Override
  public SignedReadUrl signedReadUrl(String objectKey) {
    if (!objectExists(objectKey)) {
      throw new ContentStorageException("Content object is missing");
    }
    try {
      String url =
          signingClient.getPresignedObjectUrl(
              GetPresignedObjectUrlArgs.builder()
                  .method(Method.GET)
                  .bucket(properties.bucket())
                  .object(objectKey)
                  .expiry(properties.signedReadLifetimeSeconds())
                  .build());
      return new SignedReadUrl(
          URI.create(url), Instant.now(clock).plusSeconds(properties.signedReadLifetimeSeconds()));
    } catch (ErrorResponseException
        | InsufficientDataException
        | InternalException
        | InvalidKeyException
        | InvalidResponseException
        | IOException
        | NoSuchAlgorithmException
        | ServerException
        | XmlParserException exception) {
      throw new ContentStorageException("Content signed URL generation failed", exception);
    }
  }

  private boolean bucketExists() {
    try {
      return objectClient.bucketExists(
          BucketExistsArgs.builder().bucket(properties.bucket()).build());
    } catch (ErrorResponseException
        | InsufficientDataException
        | InternalException
        | InvalidKeyException
        | InvalidResponseException
        | IOException
        | NoSuchAlgorithmException
        | ServerException
        | XmlParserException exception) {
      throw new ContentStorageException("Content storage bucket check failed", exception);
    }
  }

  private static MinioClient client(
      String endpoint, ContentStorageProperties properties, OkHttpClient httpClient) {
    return MinioClient.builder()
        .endpoint(endpoint)
        .credentials(properties.accessKey(), properties.secretKey())
        .httpClient(httpClient)
        .build();
  }

  private static OkHttpClient httpClient(ContentStorageProperties properties) {
    return new OkHttpClient.Builder()
        .connectTimeout(properties.connectTimeout().toMillis(), TimeUnit.MILLISECONDS)
        .readTimeout(properties.readTimeout().toMillis(), TimeUnit.MILLISECONDS)
        .writeTimeout(properties.writeTimeout().toMillis(), TimeUnit.MILLISECONDS)
        .build();
  }
}
