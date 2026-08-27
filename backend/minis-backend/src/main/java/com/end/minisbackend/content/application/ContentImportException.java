package com.end.minisbackend.content.application;

public class ContentImportException extends RuntimeException {
  public ContentImportException(String message) {
    super(message);
  }

  public ContentImportException(String message, Throwable cause) {
    super(message, cause);
  }
}
