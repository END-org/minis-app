package com.end.minisbackend.content.application.port;

import java.io.IOException;
import java.nio.file.Path;

public interface UploadVideoSource {
  String originalFilename();

  String contentType();

  long size();

  void transferTo(Path destination) throws IOException;
}
