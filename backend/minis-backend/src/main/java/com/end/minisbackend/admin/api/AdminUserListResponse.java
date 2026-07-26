package com.end.minisbackend.admin.api;

import java.util.List;

public record AdminUserListResponse(
    List<AdminUserResponse> users, int page, int size, boolean hasNext) {
  public AdminUserListResponse {
    users = List.copyOf(users);
  }

  @Override
  public List<AdminUserResponse> users() {
    return List.copyOf(users);
  }
}
