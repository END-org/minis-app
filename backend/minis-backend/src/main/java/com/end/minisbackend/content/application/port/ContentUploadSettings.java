package com.end.minisbackend.content.application.port;

import java.nio.file.Path;
import java.time.Duration;

public interface ContentUploadSettings {
  long maxFileSizeBytes();

  Path tempDirectory();

  Duration staleUploadAge();
}
