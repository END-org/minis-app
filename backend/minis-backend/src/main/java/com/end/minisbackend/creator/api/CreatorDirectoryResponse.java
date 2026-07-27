package com.end.minisbackend.creator.api;

import java.util.List;

public record CreatorDirectoryResponse(
    List<CreatorResponse> creators, int page, int size, boolean hasNext) {
  public CreatorDirectoryResponse {
    creators = List.copyOf(creators);
  }

  @Override
  public List<CreatorResponse> creators() {
    return List.copyOf(creators);
  }
}
