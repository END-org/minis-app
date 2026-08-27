package com.end.minisbackend.content.application;

import java.nio.file.Path;

public interface MediaProbe {
  MediaInspection inspect(Path file);
}
