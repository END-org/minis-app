package com.end.minisbackend.content.application.model.importing;

public class ContentImportException extends RuntimeException {
  public ContentImportException(String message) {
    super(message);
  }

  public ContentImportException(String message, Throwable cause) {
    super(message, cause);
  }
}
