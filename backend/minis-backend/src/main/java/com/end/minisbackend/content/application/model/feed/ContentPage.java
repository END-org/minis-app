package com.end.minisbackend.content.application.model.feed;

import java.util.List;

public record ContentPage(List<PublicContentItem> items, String nextCursor, int limit) {
  public ContentPage {
    items = items == null ? List.of() : List.copyOf(items);
  }
}
