package com.end.minisbackend.content.api;

import java.util.List;

public record ContentPageResponse(List<ContentResponse> items, String nextCursor, int limit) {
  public ContentPageResponse {
    items = items == null ? List.of() : List.copyOf(items);
  }
}
