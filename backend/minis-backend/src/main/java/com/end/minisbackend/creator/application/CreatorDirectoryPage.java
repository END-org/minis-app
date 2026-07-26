package com.end.minisbackend.creator.application;

import java.util.List;

public record CreatorDirectoryPage(
    List<CreatorProfileView> creators, int page, int size, boolean hasNext) {
  public CreatorDirectoryPage {
    creators = List.copyOf(creators);
  }

  @Override
  public List<CreatorProfileView> creators() {
    return List.copyOf(creators);
  }
}
