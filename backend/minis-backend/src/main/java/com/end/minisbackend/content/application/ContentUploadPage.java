package com.end.minisbackend.content.application;

import java.util.List;

public record ContentUploadPage(List<ContentUploadItem> items, String nextCursor, int size) {
  public ContentUploadPage {
    items = items == null ? List.of() : List.copyOf(items);
  }
}
