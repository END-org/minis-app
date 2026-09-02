package com.end.minisbackend.content.application.port;

import com.end.minisbackend.content.domain.model.media.MediaInspection;
import java.nio.file.Path;

public interface MediaProbe {
  MediaInspection inspect(Path file);
}
