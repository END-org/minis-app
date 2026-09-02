package com.end.minisbackend.content.infrastructure.media;

import java.io.IOException;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

@Component
class FfprobeProcessLauncher {
  Process start(String executable, Path file) throws IOException {
    return new ProcessBuilder(
            executable,
            "-v",
            "error",
            "-print_format",
            "json",
            "-show_format",
            "-show_streams",
            file.toString())
        .start();
  }
}
