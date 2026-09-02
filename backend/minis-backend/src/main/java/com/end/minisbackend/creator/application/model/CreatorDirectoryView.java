package com.end.minisbackend.creator.application.model;

import java.util.List;

public record CreatorDirectoryView(
    List<CreatorProfileItem> creators, int page, int size, boolean hasNext) {
  public CreatorDirectoryView {
    creators = creators == null ? List.of() : List.copyOf(creators);
  }
}
