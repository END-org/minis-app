package com.end.minisbackend.content.api.controller;

import com.end.minisbackend.content.application.port.UploadVideoSource;
import java.io.IOException;
import java.nio.file.Path;
import org.springframework.web.multipart.MultipartFile;

final class MultipartUploadVideoSource implements UploadVideoSource {
  private final MultipartFile multipartFile;

  MultipartUploadVideoSource(MultipartFile multipartFile) {
    this.multipartFile = multipartFile;
  }

  @Override
  public String originalFilename() {
    return multipartFile.getOriginalFilename();
  }

  @Override
  public String contentType() {
    return multipartFile.getContentType();
  }

  @Override
  public long size() {
    return multipartFile.getSize();
  }

  @Override
  public void transferTo(Path destination) throws IOException {
    multipartFile.transferTo(destination);
  }
}
