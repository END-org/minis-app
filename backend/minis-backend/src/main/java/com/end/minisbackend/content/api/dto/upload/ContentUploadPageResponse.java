package com.end.minisbackend.content.api.dto.upload;

import java.util.List;

public record ContentUploadPageResponse(
    List<ContentUploadResponse> items, String nextCursor, int size) {
  public ContentUploadPageResponse {
    items = items == null ? List.of() : List.copyOf(items);
  }
}
