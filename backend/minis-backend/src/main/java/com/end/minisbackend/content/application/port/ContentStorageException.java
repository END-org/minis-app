package com.end.minisbackend.content.application.port;

public class ContentStorageException extends RuntimeException {
  public ContentStorageException(String message) {
    super(message);
  }

  public ContentStorageException(String message, Throwable cause) {
    super(message, cause);
  }
}
